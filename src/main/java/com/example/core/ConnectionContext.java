package com.example.core;

import com.example.http.HTTPRequest;
import com.example.session.Session;
import com.example.config.RouteConfig;
import com.example.config.ServerConfig;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

public class ConnectionContext {
    public ByteBuffer readBuffer = ByteBuffer.allocate(8192);
    public ByteBuffer writeBuffer;

    // Raw bytes buffer for binary-safe reading
    public ByteArrayOutputStream rawBytes;

    public HTTPRequest request; 
    public ConnState state = ConnState.READING_HEADERS;

    public ServerConfig serverConfig;
    public RouteConfig matchedRoute;
    public String matchedRoot; 

    public int contentLength = 0;
    public byte[] body;

    // Chunked transfer encoding support (binary-safe)
    public boolean isChunked = false;
    public ByteArrayOutputStream chunkBodyBuffer;
    
    // Chunk parsing state machine
    public ChunkState chunkState = ChunkState.CHUNK_SIZE;
    public StringBuilder chunkSizeLine = new StringBuilder();
    public int currentChunkSize = 0;
    public int currentChunkBytesRead = 0;

    public long connectionStartTime = System.currentTimeMillis();

    public Session session;
}

enum ChunkState {
    CHUNK_SIZE,      // Reading chunk size line
    CHUNK_DATA,      // Reading chunk data
    CHUNK_CR,        // Expecting \r after chunk data
    CHUNK_LF,        // Expecting \n after chunk data
    CHUNK_TRAILER,   // Reading trailer headers (after last chunk)
    CHUNK_DONE       // All chunks received
}