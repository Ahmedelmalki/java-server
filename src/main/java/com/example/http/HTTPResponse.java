package com.example.http;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.session.Cookie;

public class HTTPResponse {
    private String version = "HTTP/1.1";
    private int statusCode = 200;
    private String statusMessage = "OK";
    private Map<String, String> headers = new HashMap<>();
    private byte[] body = new byte[0];
    private List<Cookie> cookies = new ArrayList<>();

    public void addCookie(Cookie cookie) {
        cookies.add(cookie);
    }

    public void clearCookies() {
        cookies.clear();
    }

    public List<Cookie> getCookies() {
        return cookies;
    }

    public void setStatus(int code, String message) {
        this.statusCode = code;
        this.statusMessage = message;
    }

    public int getStatusCode() {
        return this.statusCode;
    }

    public void addHeader(String k, String v) {
        headers.put(k, v);
    }

    public void setBodyBytes(byte[] data) {
        this.body = data != null ? data : new byte[0];
    }

    public void setBody(String body) {
        this.body = body.getBytes(StandardCharsets.UTF_8);
    }

    public String getBody() {
        return new String(body, StandardCharsets.UTF_8);
    }

    public byte[] getBodyBytes() {
        return body;
    }

    public int getBodyLength() {
        return body.length;
    }

    public byte[] toBytes() {
        StringBuilder sb = new StringBuilder();
        sb.append(version).append(" ")
                .append(statusCode).append(" ")
                .append(statusMessage).append("\r\n");

        for (Map.Entry<String, String> entry : headers.entrySet()) {
            sb.append(entry.getKey()).append(": ")
                    .append(entry.getValue()).append("\r\n");
        }

        for (Cookie c : cookies) {
            sb.append("Set-Cookie: ").append(c.toSetCookieHeader()).append("\r\n");
        }

        sb.append("\r\n");

        // Handle both text body and binary body
        byte[] headerBytes = sb.toString().getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[headerBytes.length + body.length];

        System.arraycopy(headerBytes, 0, result, 0, headerBytes.length);
        System.arraycopy(body, 0, result, headerBytes.length, body.length);

        return result;
    }

    @Override
    public String toString() {
        return String.format("HTTPResponse{status=%d %s, headers=%d, cookies=%d, bodyLength=%d}",
                statusCode, statusMessage, headers.size(), cookies.size(), body.length);
    }
}