package com.example.core;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;
import com.example.config.ServerConfig;
import com.example.http.HTTPRequest;
import com.example.http.HTTPResponse;

public class Server {

    private final ServerConfig config; // store full server config

    public Server(ServerConfig config) {
        this.config = config;
    }

    public void start() throws IOException {
        /*
         * Selector in Java NIO is an abstraction that multiplexes SelectableChannel
         * objects,
         * enabling efficient, non-blocking I/O operations on multiple channels using a
         * single thread
         */
        Selector selector = Selector.open();
        // ServerConfig serverConfig = new ServerConfig(/* whatever */);

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
                System.out.println("=========\nSelectionKey: " + key.toString() + "\n=========");
                it.remove();

                if (key.isAcceptable()) {
                    accept(selector, key);
                } else if (key.isReadable()) {
                    read(key);
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

        String raw = new String(data);
        if (!raw.contains("\r\n\r\n"))
            return;

        ctx.request = HTTPRequest.parse(raw);
        ctx.headresComplete = true;

        HTTPResponse res = new HTTPResponse();
        res.setStatus(200, "OK");
        res.setBody("Hello, hell!");
        res.addHeader("Content-Type", "text/plain");
        res.addHeader("Content-Length", String.valueOf(res.getBody().length()));

        client.write(ByteBuffer.wrap(res.toBytes()));
        client.close();

    }

}