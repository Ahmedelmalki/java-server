package com.example.handlers;

import com.example.http.*;
import com.example.config.RouteConfig;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class CGIHandler {

    public static HTTPResponse handle(HTTPRequest req, RouteConfig route) {
        HTTPResponse res = new HTTPResponse();

        try {
            String scriptPath = resolveScriptPath(req, route);
            String interpreter = resolveInterpreter(scriptPath, route);
            ProcessBuilder pb = new ProcessBuilder(interpreter, scriptPath);
            Map<String, String> env = pb.environment();

            // ---- CGI ENV ----
            env.put("GATEWAY_INTERFACE", "CGI/1.1");
            env.put("SERVER_PROTOCOL", "HTTP/1.1");
            env.put("REQUEST_METHOD", req.method);
            env.put("SCRIPT_FILENAME", scriptPath);
            env.put("SCRIPT_NAME", req.path);
            env.put("QUERY_STRING", extractQuery(req.path));
            env.put("CONTENT_TYPE", req.headers.getOrDefault("Content-Type", ""));
            env.put("CONTENT_LENGTH", req.headers.getOrDefault("Content-Length", ""));
            env.put("REMOTE_ADDR", "127.0.0.1");

            for (Map.Entry<String, String> h : req.headers.entrySet()) {
                String key = "HTTP_" + h.getKey().toUpperCase().replace('-', '_');
                System.out.println("key: " + key + " value: " + h.getValue());
                env.put(key, h.getValue());
            }
            pb.redirectErrorStream(true);
            Process proc = pb.start();

            if (req.body != null && req.getBodyLength() != 0) {
                try (OutputStream out = proc.getOutputStream()) {
                    out.write(req.getBodyBytes());
                }
            } else {
                proc.getOutputStream().close();
            }

            String cgiOutput;
            try (InputStream in = proc.getInputStream()) {
                cgiOutput = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }

            return parseCgiOutput(cgiOutput);
        } catch (Exception ex) {
            System.err.println("oh no: " + ex.getMessage());
            res.setStatus(500, "Internal Server Error");
            res.setBody("CGI Error");
            return res;
        }
    }

    private static String resolveScriptPath(HTTPRequest req, RouteConfig route) {
        String rel = req.path.substring(route.path.length());
        if (rel.isEmpty() || rel.equals("/")) {
            throw new RuntimeException("No CGI script specified");
        }
        return route.root + rel;
    }

    private static String resolveInterpreter(String scriptPath, RouteConfig route) {
        for (String ext : route.cgi.keySet()) {
            if (scriptPath.endsWith(ext)) {
                return route.cgi.get(ext);
            }
        }
        throw new RuntimeException("No CGI interpreter for script: " + scriptPath);
    }

    private static String extractQuery(String path) {
        int idx = path.indexOf('?');
        return idx >= 0 ? path.substring(idx + 1) : "";
    }

    // ---- Parse CGI headers ----
    private static HTTPResponse parseCgiOutput(String out) {
        HTTPResponse res = new HTTPResponse();

        String[] parts = out.split("\r?\n\r?\n", 2);
        String headerBlock = parts.length > 0 ? parts[0] : "";
        String body = parts.length > 1 ? parts[1] : "";

        int status = 200;
        String statusText = "OK";

        for (String line : headerBlock.split("\r?\n")) {
            if (line.isBlank())
                continue;

            if (line.startsWith("Status:")) {
                // Status: 302 Found
                String[] s = line.substring(7).trim().split(" ", 2);
                status = Integer.parseInt(s[0]);
                if (s.length > 1)
                    statusText = s[1];
            } else {
                int idx = line.indexOf(':');
                if (idx > 0) {
                    String k = line.substring(0, idx).trim();
                    String v = line.substring(idx + 1).trim();
                    res.addHeader(k, v);
                }
            }
        }

        res.setStatus(status, statusText);
        res.setBody(body);
        return res;
    }
}
