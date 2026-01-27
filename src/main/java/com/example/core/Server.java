package com.example.core;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;

import com.example.config.RouteConfig;
import com.example.config.ServerConfig;
import com.example.http.HTTPRequest;
import com.example.http.HTTPResponse;
import com.example.routing.Router;

public class Server {

    private final ServerConfig config; // store full server config
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
            selector.select();
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

        ctx.readBuffer.flip(); // switch buffer to read mode

        byte[] data = new byte[ctx.readBuffer.remaining()];
        ctx.readBuffer.get(data);
        System.out.println("data :\n" + data.toString());
        ctx.readBuffer.clear();

        String chunk = new String(data);
        ctx.raw.append(chunk);

        // ----- HEADER DETECTION -----
        if (ctx.state == ConnState.READING_HEADERS) {
            if (!ctx.raw.toString().contains("\r\n\r\n")) {
                // headers not complete yet
                return;
            }
            ctx.request = HTTPRequest.parse(ctx.raw.toString());
            for (RouteConfig route : ctx.serverConfig.routes) {
                if (ctx.request.path.startsWith(route.path)) {
                    ctx.matchedRoot = route.root;
                    break;
                }
            }

            ctx.state = ConnState.PROCESSING;
        }

        // ----- PROCESS REQUEST -----
        if (ctx.state == ConnState.PROCESSING) {
            HTTPResponse res = router.route(ctx.request, ctx.serverConfig);

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
}
