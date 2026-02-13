package com.example.handlers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.example.config.ServerConfig;
import com.example.http.HTTPRequest;
import com.example.http.HTTPResponse;

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

    public HTTPResponse handle408(HTTPRequest req, ServerConfig config) {
        return loadErrorPage(408, "request timeout", config);
    }

    /**
     * Load error page from config or use default message
     */
    private HTTPResponse loadErrorPage(int statusCode, String statusMessage, ServerConfig config) {
        HTTPResponse res = new HTTPResponse();
        res.setStatus(statusCode, statusMessage);
        res.addHeader("Content-Type", "text/html; charset=utf-8");
        res.addHeader("Connection", "close"); // force close

        // load custom error page from config
        String errorPagePath = (config != null && config.errorPages != null) ?
         config.errorPages.get(String.valueOf(statusCode)) : null;

        // if (config != null && config.errorPages != null) {
        //     errorPagePath = config.errorPages.get(String.valueOf(statusCode));
        // }

        if (errorPagePath != null) {
            Path file = Path.of(errorPagePath);
            if (Files.exists(file)) {
                try {
                    byte[] content = Files.readAllBytes(file);
                    res.setBodyBytes(content);
                    res.addHeader("Content-Length", String.valueOf(content.length));
                    return res;
                } catch (IOException ex) {
                    System.err.println("Failed to read error page: " + errorPagePath);
                }
            } else {
                System.err.println("Error page not found: " + errorPagePath);
            }
        }

        // Default error message (plain text)
        String defaultHtml = "<html><head><title>Error " + statusCode + "</title></head>" +
                "<body><h1>" + statusCode + "</h1>" +
                "    <p>" + statusMessage + "</p>" +
                "</body></html>";

        res.setBody(defaultHtml);
        res.addHeader("Content-Length", String.valueOf(res.getBodyLength()));
        return res;
    }
}