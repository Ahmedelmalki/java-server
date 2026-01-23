package com.example.core;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;

public class Server {

    private final int[] ports;

    public Server(int[] ports) {
        this.ports = ports;
    }

    public void start() throws IOException {
        Selector selector = Selector.open();
        for (int port : ports) {
            ServerSocketChannel server = ServerSocketChannel.open();
            server.configureBlocking(false);
            server.bind(new InetSocketAddress(port));
            server.register(selector, SelectionKey.OP_ACCEPT);
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
                }
            }
        }
    }

    public void accept(Selector selector, SelectionKey key) throws IOException {
        ServerSocketChannel server = (ServerSocketChannel) key.channel();
        SocketChannel client = server.accept();
        client.configureBlocking(false);
        client.register(selector, SelectionKey.OP_READ, ByteBuffer.allocate(8192));
        System.out.println("Accepted " + client.getRemoteAddress());
    }

    public void read(SelectionKey key) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();
        ByteBuffer buffer = (ByteBuffer) key.attachment();

        int bytesRead = client.read(buffer);

        if (bytesRead == -1) {
            client.close();
            return;
        }
        /*
        This effectively "flips" the buffer from write mode 
        (where data is being filled) to read mode 
        (where data is being extracted)
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