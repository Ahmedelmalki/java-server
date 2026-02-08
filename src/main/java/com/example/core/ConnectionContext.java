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

    /**
     * Reset context for Keep-Alive connection reuse.
     * Clears request-specific state while preserving connection-level state.
     */
    public void reset() {
        // Clear buffers
        readBuffer.clear();
        writeBuffer = null;
        rawBytes = null;
        
        // Clear request state
        request = null;
        state = ConnState.READING_HEADERS;
        
        // Clear body state
        contentLength = 0;
        body = null;
        
        // Clear chunked state
        isChunked = false;
        chunkBodyBuffer = null;
        chunkState = ChunkState.CHUNK_SIZE;
        chunkSizeLine = new StringBuilder();
        currentChunkSize = 0;
        currentChunkBytesRead = 0;
        
        // Clear route matching state
        matchedRoute = null;
        matchedRoot = null;
    }
}

enum ChunkState {
    CHUNK_SIZE,      // Reading chunk size line
    CHUNK_DATA,      // Reading chunk data
    CHUNK_CR,        // Expecting \r after chunk data
    CHUNK_LF,        // Expecting \n after chunk data
    CHUNK_TRAILER,   // Reading trailer headers (after last chunk)
    CHUNK_DONE       // All chunks received
}