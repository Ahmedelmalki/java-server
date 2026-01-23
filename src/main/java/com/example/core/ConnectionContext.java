package com.example.core;

import com.example.http.HTTPRequest;
import com.example.config.ServerConfig;
import java.nio.ByteBuffer;

public class ConnectionContext {

    public ByteBuffer readBuffer = ByteBuffer.allocate(8192);
    public ByteBuffer writeBuffer;

    public HTTPRequest request; // Parsed request

    public ServerConfig serverConfig; // virtual server this con belongs to

    public boolean headresComplete = false;
    public boolean requestComplete = false;
    public boolean keepAlive = false;

    public int contentLength = 0;
    public int bodyBytesRead = 0;
}