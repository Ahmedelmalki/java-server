package com.example.core;

import com.example.http.HTTPRequest;
import com.example.session.Session;
import com.example.config.RouteConfig;
import com.example.config.ServerConfig;
import java.nio.ByteBuffer;

public class ConnectionContext {
    public ByteBuffer readBuffer = ByteBuffer.allocate(8192);
    public ByteBuffer writeBuffer;
    public StringBuilder raw = new StringBuilder();
    public HTTPRequest request;
    public ConnState state = ConnState.READING_HEADERS;
    public ServerConfig serverConfig;
    public RouteConfig matchedRoute;
    public String matchedRoot;
    public int contentLength = 0;
    public byte[] body;
    public long connectionStartTime = System.currentTimeMillis();
    public Session session;
    public boolean isChunked = false;
    public StringBuilder chunkBuffer;
}
