package com.example.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import com.example.config.*;
import com.example.http.*;
import com.example.routing.*;
import com.example.session.SessionManager;

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

            // Cleanup sessions and timeouts in the main event loop
            checkTimeouts(selector);
            SessionManager.getInstance().cleanupExpiredSessions();

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

        // READING HEADERS
        if (ctx.state == ConnState.READING_HEADERS) {
            while (ctx.readBuffer.hasRemaining()) {
                char c = (char) ctx.readBuffer.get();
                ctx.headerBuffer.append(c);

                // Check for end of headers (\r\n\r\n)
                if (ctx.headerBuffer.toString().endsWith("\r\n\r\n")) {
                    String headerPart = ctx.headerBuffer.toString();
                    ctx.request = HTTPRequest.parse(headerPart);

                    if (ctx.request == null) {
                        sendBadRequestResponse(key);
                        return;
                    }

                    // Prepare body reader
                    String transferEncoding = ctx.request.headers.get("Transfer-Encoding");
                    String contentLength = ctx.request.headers.get("Content-Length");

                    long maxBodySize = ctx.serverConfig.clientMaxBodySize;

                    if (transferEncoding != null && transferEncoding.toLowerCase().contains("chunked")) {
                        ctx.bodyReader = new com.example.parser.ChunkedBodyReader(maxBodySize);
                        ctx.state = ConnState.READING_BODY;
                    } else if (contentLength != null) {
                        try {
                            long length = Long.parseLong(contentLength);
                            if (maxBodySize > 0 && length > maxBodySize) {
                                send413Response(key);
                                return;
                            }
                            ctx.bodyReader = new com.example.parser.FixedLengthReader(length);
                            ctx.state = ConnState.READING_BODY;
                        } catch (NumberFormatException e) {
                            sendBadRequestResponse(key);
                            return;
                        }
                    } else {
                        ctx.bodyReader = new com.example.parser.FixedLengthReader(0);
                        ctx.state = ConnState.PROCESSING;
                    }
                    break;
                }
            }
        }

        // READING BODY
        if (ctx.state == ConnState.READING_BODY) {
            try {
                if (ctx.bodyReader.process(ctx.readBuffer)) {
                    byte[] body = ctx.bodyReader.getBody();
                    ctx.request.setBody(body);
                    ctx.state = ConnState.PROCESSING;
                }
            } catch (RuntimeException e) {
                if (e.getMessage().contains("too large")) {
                    send413Response(key);
                } else {
                    sendBadRequestResponse(key);
                }
                return;
            }
        }

        // PROCESS REQUEST
        if (ctx.state == ConnState.PROCESSING) {
            HTTPResponse res = router.route(ctx.request, ctx.serverConfig, ctx.request.getBodyBytes());
            ctx.writeBuffer = ByteBuffer.wrap(res.toBytes());
            ctx.state = ConnState.WRITING_RESPONSE;
            key.interestOps(SelectionKey.OP_WRITE);
        }

        ctx.readBuffer.clear();
    }

    // Server.java - write() method
    public void write(SelectionKey key) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();
        ConnectionContext ctx = (ConnectionContext) key.attachment();

        client.write(ctx.writeBuffer);
        if (!ctx.writeBuffer.hasRemaining()) {
            // Check Connection header
            String connection = ctx.request.headers.get("Connection");

            if ("keep-alive".equalsIgnoreCase(connection)) {
                // Reset context for next request
                ctx.reset();
                key.interestOps(SelectionKey.OP_READ);
            } else {
                // Close connection
                ctx.state = ConnState.CLOSED;
                client.close();
            }
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
        res.setBody("Malformed HTTP request");
        res.addHeader("Content-Type", "text/plain");
        res.addHeader("Content-Length", String.valueOf(res.getBodyLength()));
        res.addHeader("Connection", "close");

        ctx.writeBuffer = ByteBuffer.wrap(res.toBytes());
        ctx.state = ConnState.WRITING_RESPONSE;

        client.write(ctx.writeBuffer);
        client.close();
        key.cancel();
    }

    private void send413Response(SelectionKey key) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();
        ConnectionContext ctx = (ConnectionContext) key.attachment();

        HTTPResponse res = new HTTPResponse();
        res.setStatus(413, "Payload Too Large");
        res.setBody("Request body exceeds maximum allowed size");
        res.addHeader("Content-Type", "text/plain");
        res.addHeader("Content-Length", String.valueOf(res.getBodyLength()));
        res.addHeader("Connection", "close");

        ctx.writeBuffer = ByteBuffer.wrap(res.toBytes());
        ctx.state = ConnState.WRITING_RESPONSE;

        client.write(ctx.writeBuffer);
        client.close();
        key.cancel();
    }
}