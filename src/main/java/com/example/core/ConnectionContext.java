package com.example.core;

import com.example.http.HTTPRequest;
import com.example.config.ServerConfig;
import java.nio.ByteBuffer;

public class ConnectionContext {
    public ByteBuffer readBuffer = ByteBuffer.allocate(8192);
    public ByteBuffer writeBuffer;

    public StringBuilder raw = new StringBuilder();

    public HTTPRequest request; // Parsed request
    public ConnState state = ConnState.READING_HEADERS;

    public ServerConfig serverConfig;
}