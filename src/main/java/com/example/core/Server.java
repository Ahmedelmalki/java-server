package com.example.core;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.example.config.ServerConfig;
import com.example.handlers.ErrorHandler;
import com.example.http.HTTPRequest;
import com.example.http.HTTPResponse;
import com.example.routing.Router;
import com.example.session.SessionManager;

public class Server {

    private final List<ServerConfig> serverConfigs;
    private final Map<Integer, List<ServerConfig>> configsByPort;
    private final Router router = new Router();

    public Server(List<ServerConfig> serverConfigs) {
        if (serverConfigs == null || serverConfigs.isEmpty()) {
            throw new IllegalArgumentException("At least one server configuration is required");
        }
        this.serverConfigs = List.copyOf(serverConfigs);
        this.configsByPort = buildConfigsByPort(this.serverConfigs);
    }

    public void start() throws IOException {
        //one selector
        Selector selector = Selector.open();

        for (int port : configsByPort.keySet()) {
            ServerSocketChannel server = ServerSocketChannel.open();
            server.configureBlocking(false);
            server.bind(new InetSocketAddress(port));
            server.register(selector, SelectionKey.OP_ACCEPT, port);
            System.out.println("Listening on port: " + port);
        }

        while (true) {
            selector.select(1000);

            checkTimeouts(selector);
            SessionManager.getInstance().cleanupExpiredSessions();

            Set<SelectionKey> keys = selector.selectedKeys();
            Iterator<SelectionKey> it = keys.iterator();

            while (it.hasNext()) {
                SelectionKey key = it.next();
                it.remove(); // process and remove  the key

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
        Integer port = (Integer) key.attachment();

        SocketChannel client = server.accept();
        if (client == null) { // checked properly
            return;
        }

        client.configureBlocking(false);

        ConnectionContext ctx = new ConnectionContext();
        ctx.localPort = port;
        ctx.serverConfig = getDefaultServerForPort(port);  

        client.register(selector, SelectionKey.OP_READ, ctx); //attach context to  socket
        System.out.println("Accepted " + client.getRemoteAddress());
    }

    public void read(SelectionKey key) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();
        ConnectionContext ctx = (ConnectionContext) key.attachment(); // get context

        int bytesRead = client.read(ctx.readBuffer);

        if (bytesRead == -1) { // checked properly
            client.close(); // remove client conn
            return;
        }

        ctx.readBuffer.flip();

        if (ctx.state == ConnState.READING_HEADERS) {
            while (ctx.readBuffer.hasRemaining()) {
                char c = (char) ctx.readBuffer.get();
                ctx.headerBuffer.append(c);

                if (ctx.headerBuffer.toString().endsWith("\r\n\r\n")) {
                    String headerPart = ctx.headerBuffer.toString();
                    ctx.request = HTTPRequest.parse(headerPart);

                    if (ctx.request == null) {
                        sendBadRequestResponse(key);
                        return;
                    }

                    ctx.serverConfig = resolveServerConfig(ctx.request, ctx.localPort);

                    String transferEncoding = ctx.request.headers.get("Transfer-Encoding");
                    String contentLength = ctx.request.headers.get("Content-Length");

                    long maxBodySize = ctx.serverConfig.clientMaxBodySize;

                    if (transferEncoding != null && transferEncoding.toLowerCase(Locale.ROOT).contains("chunked")) {
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

        if (ctx.state == ConnState.PROCESSING) {
            HTTPResponse res = router.route(ctx.request, ctx.serverConfig, ctx.request.getBodyBytes());
            ctx.writeBuffer = ByteBuffer.wrap(res.toBytes());
            ctx.state = ConnState.WRITING_RESPONSE;
            key.interestOps(SelectionKey.OP_WRITE);

            if (res.getStatusCode() >= 400) {
                res.addHeader("Connection", "close"); // force close
            }
        }

        ctx.readBuffer.clear();
    }

    public void write(SelectionKey key) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();
        ConnectionContext ctx = (ConnectionContext) key.attachment();
        if (ctx.writeBuffer == null) {
            return;
        }

        client.write(ctx.writeBuffer);

        if (!ctx.writeBuffer.hasRemaining()) {
            String resStart = new String(ctx.writeBuffer.array(), 0, 15);
            boolean isErr = resStart.contains(" 4") || resStart.contains(" 5");

            if (isErr || "close".equalsIgnoreCase(ctx.request.headers.get("Connection"))) {
                client.close(); // remove client conn
                key.cancel();
            } else {
                ctx.reset();
                key.interestOps(SelectionKey.OP_READ);
            }
        }
    }

    private void checkTimeouts(Selector selector) {
        long currentTime = System.currentTimeMillis();

        for (SelectionKey key : selector.keys()) {
            if (key.channel() instanceof ServerSocketChannel) {
                continue;
            }

            if (key.attachment() instanceof ConnectionContext) {
                ConnectionContext ctx = (ConnectionContext) key.attachment();
                long elapsed = currentTime - ctx.connectionStartTime;
                long timeout = ctx.serverConfig != null ? ctx.serverConfig.timeout : 30000L;

                if (elapsed > timeout) {
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

    //multi port
    private Map<Integer, List<ServerConfig>> buildConfigsByPort(List<ServerConfig> configs) {
        Map<Integer, List<ServerConfig>> byPort = new LinkedHashMap<>();

        for (ServerConfig config : configs) {
            Set<Integer> seenInConfig = new HashSet<>();
            for (int port : config.ports) {
                if (!seenInConfig.add(port)) {
                    throw new IllegalArgumentException("Duplicate port " + port + " in one server block");
                }
                byPort.computeIfAbsent(port, p -> new ArrayList<>()).add(config);
            }
        }

        return byPort;
    }

    private ServerConfig getDefaultServerForPort(int port) {
        List<ServerConfig> candidates = configsByPort.get(port);
        if (candidates == null || candidates.isEmpty()) {
            return serverConfigs.get(0);
        }

        for (ServerConfig config : candidates) {
            if (config.isDefault) {
                return config;
            }
        }

        return candidates.get(0);
    }

    private ServerConfig resolveServerConfig(HTTPRequest request, int port) {
        List<ServerConfig> candidates = configsByPort.get(port);
        if (candidates == null || candidates.isEmpty()) {
            return serverConfigs.get(0);
        }

        String hostHeader = request.headers.get("Host");
        String normalizedHost = extractHostName(hostHeader);

        if (!normalizedHost.isEmpty()) {
            for (ServerConfig config : candidates) {
                if (hostMatches(normalizedHost, config.serverName)) {
                    return config;
                }
            }
            for (ServerConfig config : candidates) {
                if (hostMatches(normalizedHost, config.host)) {
                    return config;
                }
            }
        }

        return getDefaultServerForPort(port);
    }

    private String extractHostName(String hostHeader) {
        if (hostHeader == null) {
            return "";
        }

        String value = hostHeader.trim();
        if (value.isEmpty()) {
            return "";
        }

        if (value.startsWith("[")) {
            int closing = value.indexOf(']');
            if (closing > 1) {
                return value.substring(1, closing).toLowerCase(Locale.ROOT);
            }
            return value.toLowerCase(Locale.ROOT);
        }

        int colon = value.indexOf(':');
        if (colon >= 0) {
            return value.substring(0, colon).trim().toLowerCase(Locale.ROOT);
        }

        return value.toLowerCase(Locale.ROOT);
    }

    private boolean hostMatches(String requestHost, String configuredHost) {
        if (configuredHost == null) {
            return false;
        }
        return Objects.equals(requestHost, configuredHost.trim().toLowerCase(Locale.ROOT));
    }

    private void sendTimeoutResponse(SelectionKey key) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();
        ConnectionContext ctx = (ConnectionContext) key.attachment();

        if (ctx.state == ConnState.CLOSED || ctx.writeBuffer != null) {
            client.close();
            key.cancel();
            return;
        }

        ErrorHandler errorHandler = new ErrorHandler();
        HTTPResponse res = errorHandler.handle408(ctx.request, ctx.serverConfig);

        ctx.writeBuffer = ByteBuffer.wrap(res.toBytes());
        ctx.state = ConnState.WRITING_RESPONSE;

        client.write(ctx.writeBuffer);
        client.close();
        key.cancel();
    }

    private void sendBadRequestResponse(SelectionKey key) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();
        ConnectionContext ctx = (ConnectionContext) key.attachment();

        ErrorHandler errorHandler = new ErrorHandler();
        HTTPResponse res = errorHandler.handle400(ctx.request, ctx.serverConfig);
        // res.setStatus(400, "Bad Request");
        // res.setBody("Malformed HTTP request");
        // res.addHeader("Content-Type", "text/plain");
        // res.addHeader("Content-Length", String.valueOf(res.getBodyLength()));
        // res.addHeader("Connection", "close");

        ctx.writeBuffer = ByteBuffer.wrap(res.toBytes());
        ctx.state = ConnState.WRITING_RESPONSE;

        client.write(ctx.writeBuffer);
        client.close();
        key.cancel();
    }

    private void send413Response(SelectionKey key) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();
        ConnectionContext ctx = (ConnectionContext) key.attachment();

        ErrorHandler errorHandler = new ErrorHandler();
        HTTPResponse res = errorHandler.handle413(ctx.request, ctx.serverConfig);
        // res.addHeader("Connection", "close");

        ctx.writeBuffer = ByteBuffer.wrap(res.toBytes());
        ctx.state = ConnState.WRITING_RESPONSE;

        client.write(ctx.writeBuffer);
        client.close();
        key.cancel();
    }
}
