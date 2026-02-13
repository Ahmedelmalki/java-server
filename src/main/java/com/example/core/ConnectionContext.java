package com.example.core;

import java.nio.ByteBuffer;

import com.example.config.RouteConfig;
import com.example.config.ServerConfig;
import com.example.http.HTTPRequest;
import com.example.parser.BodyReader;
import com.example.session.Session;

public class ConnectionContext {
    public ByteBuffer readBuffer = ByteBuffer.allocate(8192);
    public ByteBuffer writeBuffer;

    public HTTPRequest request; 
    public ConnState state = ConnState.READING_HEADERS;

    public ServerConfig serverConfig;
    public int localPort;
    public RouteConfig matchedRoute;
    public String matchedRoot; 

    // REMOVE all chunk-related fields and replace with:
    public BodyReader bodyReader;  
    public StringBuilder headerBuffer = new StringBuilder(); 

    public long connectionStartTime = System.currentTimeMillis();
    public Session session;

    public void reset() {
        readBuffer.clear();
        writeBuffer = null;
        
        if (bodyReader != null) {
            bodyReader.cleanup();
            bodyReader = null;
        }
        
        request = null;
        state = ConnState.READING_HEADERS;
        headerBuffer = new StringBuilder();
        matchedRoute = null;
        matchedRoot = null;
    }
}
