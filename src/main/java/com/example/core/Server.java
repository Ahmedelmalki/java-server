package com.example.core;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.example.config.ServerConfig;

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
        // ByteBuffer buffer = (ByteBuffer) key.attachment();
        ConnectionContext ctx = (ConnectionContext) key.attachment();
        ByteBuffer buffer = ctx.readBuffer;

        int bytesRead = client.read(buffer);

        if (bytesRead == -1) { // end-of-stream
            client.close();
            return;
        }
        /*
         * This effectively "flips" the buffer from write mode
         * (where data is being filled) to read mode
         * (where data is being extracted)
         */
        buffer.flip();
        byte[] data = new byte[buffer.remaining()];
        buffer.get(data);
        buffer.clear();

        String request = new String(data);
        System.out.println("Request:\n" + request);

        String response = "HTTP/1.1 200 OK\r\n" +
                "Content-Length: 5\r\n" +
                "Connection: close\r\n" +
                "\r\n" +
                "Hello";

        client.write(ByteBuffer.wrap(response.getBytes()));
        client.close();
    }

}