package com.example.handlers;

import com.example.http.HTTPRequest;
import com.example.http.HTTPResponse;
import com.example.config.ServerConfig;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ErrorHandler {

    public HTTPResponse handle404(HTTPRequest req, ServerConfig config) {
        return loadErrorPage(404, "Not Found", config);
    }

    public HTTPResponse handle405(HTTPRequest req, ServerConfig config) {
        return loadErrorPage(405, "Method Not Allowed", config);
    }

    public HTTPResponse handle500(HTTPRequest req, ServerConfig config) {
        return loadErrorPage(500, "Internal Server Error", config);
    }

    public HTTPResponse handle400(HTTPRequest req, ServerConfig config) {
        return loadErrorPage(400, "Bad Request", config);
    }

    public HTTPResponse handle403(HTTPRequest req, ServerConfig config) {
        return loadErrorPage(403, "Forbidden", config);
    }

    public HTTPResponse handle413(HTTPRequest req, ServerConfig config) {
        return loadErrorPage(413, "Payload Too Large", config);
    }

    /**
     * Load error page from config or use default message
     */
    private HTTPResponse loadErrorPage(int statusCode, String statusMessage, ServerConfig config) {
        HTTPResponse res = new HTTPResponse();
        res.setStatus(statusCode, statusMessage);

        // Try to load custom error page from config
        String errorPagePath = null;
        if (config.errorPages != null) {
            errorPagePath = config.errorPages.get(String.valueOf(statusCode));
        }

        if (errorPagePath != null) {
            Path file = Path.of(errorPagePath);

            if (Files.exists(file)) {
                try {
                    String content = Files.readString(file);
                    res.setBody(content);
                    res.addHeader("Content-Type", "text/html; charset=utf-8");
                    res.addHeader("Content-Length", String.valueOf(res.getBodyLength()));
                    return res;
                } catch (IOException ex) {
                    System.err.println("Failed to read error page: " + errorPagePath);
                    // Fall through to default error message
                }
            } else {
                System.err.println("Error page not found: " + errorPagePath);
                // Fall through to default error message
            }
        }

        // Default error message (plain text)
        res.setBody(statusCode + " " + statusMessage);
        res.addHeader("Content-Type", "text/plain");
        res.addHeader("Content-Length", String.valueOf(res.getBodyLength()));

        return res;
    }
}