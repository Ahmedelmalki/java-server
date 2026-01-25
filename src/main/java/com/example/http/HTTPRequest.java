package com.example.http;

import java.util.Map;
import java.util.HashMap;

public class HTTPRequest {
    public String method;
    public String path;
    public String version;
    public Map<String, String> headers = new HashMap<>();

    public static HTTPRequest parse(String raw) {
        HTTPRequest req = new HTTPRequest();

        String[] lines = raw.split("\r\n");
        if (lines.length == 0)
            return null;

        // Request line
        String[] parts = lines[0].split(" ");
        req.method = parts[0];
        req.path = parts[1];
        req.version = parts[2];

        // Headers
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            if (line.isEmpty())
                break;

            int idx = line.indexOf(":");
            if (idx > 0) {
                String k = line.substring(0, idx);
                String v = line.substring(idx + 1).trim();
                req.headers.put(k, v);
            }
        }

        return req;
    }
}