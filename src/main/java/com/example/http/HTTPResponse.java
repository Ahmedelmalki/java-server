package com.example.http;

import java.util.Map;
import java.util.HashMap;

public class HTTPResponse {

    private String version = "HTTP/1.1";
    private int statusCode = 200;
    private String statusMessage = "OK";
    private Map<String, String> headers = new HashMap<>();
    private String body = "";

    public void setStatus(int code, String message) {
        this.statusCode = code;
        this.statusMessage = message;
    }

    public void addHeader(String k, String v) {
        headers.put(k, v);
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getBody(){
        return this.body;
    }

    public byte[] toBytes() {
        StringBuilder sb = new StringBuilder();
        sb.append(version).append(" ")
                .append(statusCode).append(" ")
                .append(statusMessage).append("\r\n");

        for(Map.Entry<String, String> entry: headers.entrySet()){
            sb.append(entry.getKey()).append(": ")
            .append(entry.getValue()).append("\r\n");
        }
        
        sb.append("\r\n");
        sb.append(body);
        return sb.toString().getBytes();
    }
}