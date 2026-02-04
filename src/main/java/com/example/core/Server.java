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

        byte[] data = new byte[ctx.readBuffer.remaining()];
        ctx.readBuffer.get(data);
        ctx.readBuffer.clear();

        // Append to raw byte buffer
        if (ctx.rawBytes == null) {
            ctx.rawBytes = new ByteArrayOutputStream();
        }
        ctx.rawBytes.write(data, 0, data.length);

        // ----- READING HEADERS -----
        if (ctx.state == ConnState.READING_HEADERS) {
            byte[] allBytes = ctx.rawBytes.toByteArray();
            int headerEnd = findHeaderEnd(allBytes);

            if (headerEnd == -1)
                return; // not complete yet

            // Extract headers as string
            byte[] headerBytes = new byte[headerEnd + 4];
            System.arraycopy(allBytes, 0, headerBytes, 0, headerEnd + 4);
            String headerPart = new String(headerBytes);

            ctx.request = HTTPRequest.parse(headerPart);

            if (ctx.request == null) {
                sendBadRequestResponse(key);
                return;
            }

            String transferEncoding = ctx.request.headers.get("Transfer-Encoding");
            String cl = ctx.request.headers.get("Content-Length");

            // Check for chunked transfer encoding
            if (transferEncoding != null && transferEncoding.toLowerCase().contains("chunked")) {
                ctx.isChunked = true;
                ctx.state = ConnState.READING_BODY;
                ctx.chunkBodyBuffer = new ByteArrayOutputStream();
                System.out.println("Chunked transfer encoding detected");
            } else if (cl != null) {
                try {
                    ctx.contentLength = Integer.parseInt(cl);

                    // ===== ENFORCE CLIENT BODY SIZE LIMIT =====
                    long maxBodySize = ctx.serverConfig.clientMaxBodySize;
                    if (maxBodySize > 0 && ctx.contentLength > maxBodySize) {
                        System.err.println(
                                "Request body too large: " + ctx.contentLength + " bytes (max: " + maxBodySize + ")");
                        send413Response(key);
                        return;
                    }
                    // ==========================================

                    ctx.state = ConnState.READING_BODY;
                } catch (NumberFormatException e) {
                    sendBadRequestResponse(key);
                    return;
                }
            } else {
                ctx.contentLength = 0;
                ctx.body = new byte[0];
                ctx.state = ConnState.PROCESSING;
            }

            // Remove headers from buffer, keep only body data
            byte[] remaining = new byte[allBytes.length - (headerEnd + 4)];
            System.arraycopy(allBytes, headerEnd + 4, remaining, 0, remaining.length);
            ctx.rawBytes = new ByteArrayOutputStream();
            ctx.rawBytes.write(remaining, 0, remaining.length);
        }

        // ----- READING BODY -----
        if (ctx.state == ConnState.READING_BODY) {
            if (ctx.isChunked) {
                // Handle chunked transfer encoding with state machine
                ChunkParseResult result = readChunkedBodyIncremental(ctx);
                if (result == ChunkParseResult.DONE) {
                    ctx.state = ConnState.PROCESSING;
                } else if (result == ChunkParseResult.ERROR) {
                    sendBadRequestResponse(key);
                    return;
                } else if (result == ChunkParseResult.TOO_LARGE) {
                    send413Response(key);
                    return;
                }
                // NEED_MORE_DATA: just return and wait for next read
            } else {
                // Handle regular body with Content-Length
                byte[] current = ctx.rawBytes.toByteArray();
                if (current.length < ctx.contentLength)
                    return; // still waiting for full body

                ctx.body = new byte[ctx.contentLength];
                System.arraycopy(current, 0, ctx.body, 0, ctx.contentLength);
                ctx.state = ConnState.PROCESSING;
            }
        }

        // ----- PROCESS REQUEST -----
        if (ctx.state == ConnState.PROCESSING) {
            ctx.request.setBody(ctx.body);

            HTTPResponse res = router.route(ctx.request, ctx.serverConfig, ctx.body);

            ctx.writeBuffer = ByteBuffer.wrap(res.toBytes());
            ctx.state = ConnState.WRITING_RESPONSE;
            key.interestOps(SelectionKey.OP_WRITE);
        }
    }

    private enum ChunkParseResult {
        NEED_MORE_DATA,
        DONE,
        ERROR,
        TOO_LARGE
    }

    private ChunkParseResult readChunkedBodyIncremental(ConnectionContext ctx) throws IOException {
        byte[] buffer = ctx.rawBytes.toByteArray();
        int pos = 0;
        int len = buffer.length;

        while (pos < len) {
            byte b = buffer[pos];

            switch (ctx.chunkState) {
                case CHUNK_SIZE:
                    if (b == '\r') {
                        // End of chunk size line, expect \n next
                        pos++;
                        ctx.chunkState = ChunkState.CHUNK_SIZE;
                        // Check for \n
                        if (pos >= len) {
                            // Need more data
                            ctx.rawBytes = new ByteArrayOutputStream();
                            ctx.rawBytes.write(buffer, pos - 1, len - (pos - 1));
                            return ChunkParseResult.NEED_MORE_DATA;
                        }
                        if (buffer[pos] != '\n') {
                            System.err.println("Expected \\n after \\r in chunk size line");
                            return ChunkParseResult.ERROR;
                        }
                        pos++;

                        // Parse chunk size
                        String sizeStr = ctx.chunkSizeLine.toString().trim();
                        // Handle chunk extensions
                        int semicolon = sizeStr.indexOf(';');
                        if (semicolon != -1) {
                            sizeStr = sizeStr.substring(0, semicolon);
                        }

                        try {
                            ctx.currentChunkSize = Integer.parseInt(sizeStr, 16);
                        } catch (NumberFormatException e) {
                            System.err.println("Invalid chunk size: " + sizeStr);
                            return ChunkParseResult.ERROR;
                        }

                        ctx.chunkSizeLine = new StringBuilder();
                        ctx.currentChunkBytesRead = 0;

                        if (ctx.currentChunkSize == 0) {
                            // Last chunk
                            ctx.chunkState = ChunkState.CHUNK_TRAILER;
                        } else {
                            // Check body size limit
                            int currentSize = ctx.chunkBodyBuffer.size();
                            if (ctx.serverConfig.clientMaxBodySize > 0 &&
                                    currentSize + ctx.currentChunkSize > ctx.serverConfig.clientMaxBodySize) {
                                System.err.println("Chunked body exceeds max size");
                                return ChunkParseResult.TOO_LARGE;
                            }
                            ctx.chunkState = ChunkState.CHUNK_DATA;
                        }
                    } else if (b == '\n') {
                        // Invalid: \n without \r
                        System.err.println("Invalid chunk size line: \\n without \\r");
                        return ChunkParseResult.ERROR;
                    } else {
                        // Accumulate chunk size character
                        ctx.chunkSizeLine.append((char) b);
                        pos++;
                    }
                    break;

                case CHUNK_DATA:
                    // Read chunk data bytes
                    int bytesNeeded = ctx.currentChunkSize - ctx.currentChunkBytesRead;
                    int bytesAvailable = len - pos;
                    int bytesToRead = Math.min(bytesNeeded, bytesAvailable);

                    if (bytesToRead > 0) {
                        ctx.chunkBodyBuffer.write(buffer, pos, bytesToRead);
                        ctx.currentChunkBytesRead += bytesToRead;
                        pos += bytesToRead;
                    }

                    if (ctx.currentChunkBytesRead == ctx.currentChunkSize) {
                        // Finished reading chunk data, expect \r\n
                        ctx.chunkState = ChunkState.CHUNK_CR;
                    }
                    break;

                case CHUNK_CR:
                    if (b != '\r') {
                        System.err.println("Expected \\r after chunk data");
                        return ChunkParseResult.ERROR;
                    }
                    pos++;
                    ctx.chunkState = ChunkState.CHUNK_LF;
                    break;

                case CHUNK_LF:
                    if (b != '\n') {
                        System.err.println("Expected \\n after chunk data \\r");
                        return ChunkParseResult.ERROR;
                    }
                    pos++;
                    // Move to next chunk
                    ctx.chunkState = ChunkState.CHUNK_SIZE;
                    break;

                case CHUNK_TRAILER:
                    // Read trailer headers until we find \r\n\r\n
                    // For now, simple implementation: look for blank line
                    if (b == '\r') {
                        pos++;
                        if (pos >= len) {
                            // Need more data
                            ctx.rawBytes = new ByteArrayOutputStream();
                            ctx.rawBytes.write(buffer, pos - 1, len - (pos - 1));
                            return ChunkParseResult.NEED_MORE_DATA;
                        }
                        if (buffer[pos] == '\n') {
                            pos++;
                            // Check if this is the blank line (second CRLF)
                            if (pos >= len) {
                                // Need more data to check
                                ctx.rawBytes = new ByteArrayOutputStream();
                                ctx.rawBytes.write(buffer, pos - 2, len - (pos - 2));
                                return ChunkParseResult.NEED_MORE_DATA;
                            }
                            // Simple trailer handling: just skip until blank line
                            // Proper implementation would parse trailer headers
                            ctx.chunkState = ChunkState.CHUNK_DONE;
                        }
                    } else {
                        // Skip trailer header characters
                        pos++;
                    }
                    break;

                case CHUNK_DONE:
                    // All chunks received
                    ctx.body = ctx.chunkBodyBuffer.toByteArray();
                    System.out.println("Chunked body complete: " + ctx.body.length + " bytes");
                    // Clear buffer
                    ctx.rawBytes = new ByteArrayOutputStream();
                    return ChunkParseResult.DONE;
            }
        }

        // Processed all available data, clear buffer and wait for more
        ctx.rawBytes = new ByteArrayOutputStream();
        return ChunkParseResult.NEED_MORE_DATA;
    }

    private int findHeaderEnd(byte[] data) {
        for (int i = 0; i < data.length - 3; i++) {
            if (data[i] == '\r' && data[i + 1] == '\n' &&
                    data[i + 2] == '\r' && data[i + 3] == '\n') {
                return i;
            }
        }
        return -1;
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