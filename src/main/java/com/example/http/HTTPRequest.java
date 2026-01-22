package com.example.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;

public class HTTPRequest {
    public String method;
    public String path;
    public String version;
    public Map<String, String> headers = new HashMap<>();

    public static HTTPRequest parse(BufferedReader br) throws IOException{
        HTTPRequest req = new HTTPRequest();
        String requestLine = br.readLine();
        if (requestLine == null || requestLine.isEmpty()) return null;

        String[] parts = requestLine.split(" ");
        req.method = parts[0];
        req.path = parts[1];
        req.version = parts[2];

        String line;
        while((line = br.readLine()) != null && !line.isEmpty()){
            int idx = line.indexOf(":");
            if(idx > 0){
                String k = line.substring(0, idx);
                String v = line.substring(idx+1).trim();
                req.headers.put(k, v);
            }
        }
        return req;
    }
}