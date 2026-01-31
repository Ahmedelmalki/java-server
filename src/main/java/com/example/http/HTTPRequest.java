package com.example.http;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.HashMap;

public class HTTPRequest {
    public String method;
    public String path;
    public String version;
    public Map<String, String> headers = new HashMap<>();
    public byte[] body = new byte[0]; // Single body field as bytes

    /**
     * Parse HTTP request from raw string
     */
    public static HTTPRequest parse(String raw) {
        HTTPRequest req = new HTTPRequest();

        if (raw == null || raw.isEmpty()) {
            System.err.println("Empty request received");
            return null;
        }

        String[] lines = raw.split("\r\n");
        if (lines.length == 0) {
            System.err.println("No lines in request");
            return null;
        }

        // Request line: GET /path HTTP/1.1
        String[] parts = lines[0].split(" ");
        if (parts.length < 3) {
            System.err.println("Malformed request line: " + lines[0]);
            return null;
        }

        req.method = parts[0].trim();
        req.path = parts[1].trim();
        req.version = parts[2].trim();

        // Validate HTTP version
        if (!req.version.equals("HTTP/1.1") && !req.version.equals("HTTP/1.0")) {
            System.err.println("Unsupported HTTP version: " + req.version);
            return null;
        }

        // Validate HTTP method
        if (!isValidMethod(req.method)) {
            System.err.println("Invalid HTTP method: " + req.method);
            return null;
        }

        // Parse headers
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            if (line.isEmpty())
                break;

            int idx = line.indexOf(":");
            if (idx > 0) {
                String k = line.substring(0, idx).trim();
                String v = line.substring(idx + 1).trim();
                req.headers.put(k, v);
            }
        }

        // HTTP/1.1 requires Host header (but don't fail if missing)
        if (req.version.equals("HTTP/1.1") && !req.headers.containsKey("Host")) {
            System.err.println("HTTP/1.1 request missing Host header");
        }

        return req;
    }

    /**
     * Set body from byte array
     */
    public void setBody(byte[] body) {
        this.body = body != null ? body : new byte[0];
    }

    /**
     * Get body as byte array
     */
    public byte[] getBodyBytes() {
        return body;
    }

    /**
     * Get body as String
     */
    public String getBodyAsString() {
        return new String(body, StandardCharsets.UTF_8);
    }

    /**
     * Get body length
     */
    public int getBodyLength() {
        return body.length;
    }

    /**
     * Check if request has a body
     */
    public boolean hasBody() {
        return body.length > 0;
    }

    /**
     * Get query string from path (e.g., /page?name=value)
     */
    public String getQueryString() {
        int idx = path.indexOf('?');
        return idx >= 0 ? path.substring(idx + 1) : "";
    }

    /**
     * Get path without query string
     */
    public String getPathOnly() {
        int idx = path.indexOf('?');
        return idx >= 0 ? path.substring(0, idx) : path;
    }

    /**
     * Parse query parameters
     */
    public Map<String, String> getQueryParams() {
        Map<String, String> params = new HashMap<>();
        String query = getQueryString();
        
        if (query.isEmpty()) {
            return params;
        }

        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                params.put(urlDecode(kv[0]), urlDecode(kv[1]));
            } else if (kv.length == 1) {
                params.put(urlDecode(kv[0]), "");
            }
        }

        return params;
    }

    /**
     * Simple URL decode
     */
    private static String urlDecode(String value) {
        try {
            return java.net.URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return value;
        }
    }

    /**
     * Check if method is valid
     */
    private static boolean isValidMethod(String method) {
        return method.equals("GET") || 
               method.equals("POST") || 
               method.equals("DELETE") || 
               method.equals("PUT") || 
               method.equals("HEAD") || 
               method.equals("OPTIONS") || 
               method.equals("PATCH") ||
               method.equals("TRACE") ||
               method.equals("CONNECT");
    }

    @Override
    public String toString() {
        return String.format("HTTPRequest{method='%s', path='%s', version='%s', headers=%d, bodyLength=%d}",
                method, path, version, headers.size(), body.length);
    }
}