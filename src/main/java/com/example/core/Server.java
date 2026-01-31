package com.example.core;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import com.example.config.*;
import com.example.http.*;
import com.example.routing.*;

public class Server {

    private final ServerConfig config;
    private final Router router = new Router();

    public Server(ServerConfig config) {
        this.config = config;
    }

    public void start() throws IOException {
        Selector selector = Selector.open();

        for (int port : config.ports) {
            ServerSocketChannel server = ServerSocketChannel.open();
            server.configureBlocking(false);
            server.bind(new InetSocketAddress(port));
            server.register(selector, SelectionKey.OP_ACCEPT, config);
            System.out.println("Listening on port: " + port);
        }

        while (true) {
            selector.select(1000);
            checkTimeouts(selector);

            Set<SelectionKey> keys = selector.selectedKeys();
            Iterator<SelectionKey> it = keys.iterator();

            while (it.hasNext()) {
                SelectionKey key = it.next();
                it.remove();

                if (key.isAcceptable()) {
                    accept(selector, key);
                } else if (key.isReadable()) {
                    read(key);
                } else if (key.isWritable()) {
                    write(key);
                }
            }
        }
    }

    public void accept(Selector selector, SelectionKey key) throws IOException {
        ServerSocketChannel server = (ServerSocketChannel) key.channel();

        ServerConfig serverConfig = (ServerConfig) key.attachment();

        SocketChannel client = server.accept();
        client.configureBlocking(false);

        ConnectionContext ctx = new ConnectionContext();
        ctx.serverConfig = serverConfig;

        client.register(selector, SelectionKey.OP_READ, ctx);
        System.out.println("Accepted " + client.getRemoteAddress());
    }

    public void read(SelectionKey key) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();
        ConnectionContext ctx = (ConnectionContext) key.attachment();

        int bytesRead = client.read(ctx.readBuffer);

        if (bytesRead == -1) {
            client.close();
            return;
        }

        ctx.readBuffer.flip();

        byte[] data = new byte[ctx.readBuffer.remaining()];
        ctx.readBuffer.get(data);
        ctx.readBuffer.clear();

        String chunk = new String(data);
        ctx.raw.append(chunk);

        // ----- READING HEADERS -----
        if (ctx.state == ConnState.READING_HEADERS) {
            int headerEnd = ctx.raw.indexOf("\r\n\r\n");
            if (headerEnd == -1)
                return; // not compelet yet

            String headerPart = ctx.raw.substring(0, headerEnd + 4);
            ctx.request = HTTPRequest.parse(headerPart);

            if (ctx.request == null) {
                sendBadRequestResponse(key);
                return;
            }

            String cl = ctx.request.headers.get("Content-Length");

            if (cl != null) {
                ctx.contentLength = Integer.parseInt(cl);
                ctx.state = ConnState.READING_BODY;
            } else {
                ctx.contentLength = 0;
                ctx.body = new byte[0];
                ctx.state = ConnState.PROCESSING;
            }

            ctx.raw.delete(0, headerEnd + 4);
        }

        // ----- READING BODY -----
        if (ctx.state == ConnState.READING_BODY) {
            byte[] current = ctx.raw.toString().getBytes();
            if (current.length < ctx.contentLength)
                return; // still waiting for full body

            ctx.body = new byte[ctx.contentLength];
            System.arraycopy(current, 0, ctx.body, 0, ctx.contentLength);
            ctx.state = ConnState.PROCESSING;
        }

        // ----- PROCESS REQUEST -----
        if (ctx.state == ConnState.PROCESSING) {
            ctx.request.setBody(ctx.body);

            HTTPResponse res = router.route(ctx.request, ctx.serverConfig, ctx.body);
            // System.out.println("$$$ body: " + ctx.body.toString());

            ctx.writeBuffer = ByteBuffer.wrap(res.toBytes());
            ctx.state = ConnState.WRITING_RESPONSE;
            key.interestOps(SelectionKey.OP_WRITE);
        }
    }

    public void write(SelectionKey key) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();
        ConnectionContext ctx = (ConnectionContext) key.attachment();

        client.write(ctx.writeBuffer);
        if (!ctx.writeBuffer.hasRemaining()) {
            ctx.state = ConnState.CLOSED;
            client.close();
        }
    }

    private void checkTimeouts(Selector selector) {
        long currentTime = System.currentTimeMillis();

        for (SelectionKey key : selector.keys()) {
            if (key.channel() instanceof ServerSocketChannel) {
                continue; // skip server socket channel
            }

            if (key.attachment() instanceof ConnectionContext) {
                ConnectionContext ctx = (ConnectionContext) key.attachment();
                long elapsed = currentTime - ctx.connectionStartTime;

                if (elapsed > ctx.serverConfig.timeout) {
                    System.out.println("Connection timed out after " + elapsed + "ms");

                    try {
                        if (ctx.state != ConnState.WRITING_RESPONSE && ctx.state != ConnState.CLOSED) {
                            sendTimeoutResponse(key);
                        } else {
                            key.channel().close();
                            key.cancel();
                        }
                    } catch (IOException ex) {
                        System.err.println("error closing connection" + ex.getMessage());
                        key.cancel();
                    }
                }
            }
        }
    }

    private void sendTimeoutResponse(SelectionKey key) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();
        ConnectionContext ctx = (ConnectionContext) key.attachment();

        HTTPResponse res = new HTTPResponse();
        res.setStatus(408, "Request Timeout");
        res.setBody("Request Timeout");
        res.addHeader("Content-Type", "text/plain");
        res.addHeader("Content-Length", String.valueOf(res.getBody().length()));
        res.addHeader("Connection", "close");

        ctx.writeBuffer = ByteBuffer.wrap(res.toBytes());
        ctx.state = ConnState.WRITING_RESPONSE;

        client.write(ctx.writeBuffer);
        client.close();
        key.cancel();
    }

    private void sendBadRequestResponse(SelectionKey key) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();
        ConnectionContext ctx = (ConnectionContext) key.attachment();

        HTTPResponse res = new HTTPResponse();
        res.setStatus(400, "Bad Request");
        res.setBody("Malformed HTTP request"); // setBody() converts to bytes
        res.addHeader("Content-Type", "text/plain");
        res.addHeader("Content-Length", String.valueOf(res.getBodyLength())); // getBodyLength() returns bytes.length
        res.addHeader("Connection", "close");

        ctx.writeBuffer = ByteBuffer.wrap(res.toBytes());
        ctx.state = ConnState.WRITING_RESPONSE;

        client.write(ctx.writeBuffer);
        client.close();
        key.cancel();
    }
}
