package com.example.handlers;

import com.example.http.HTTPRequest;
import com.example.http.HTTPResponse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ErrorHandler {

    private final String errorDir = "error_pages";

    public HTTPResponse handle404(HTTPRequest req) {
        return loadErrorPage("404.html", "404 Not Found");
    }

    public HTTPResponse handle405(HTTPRequest req) {
        return loadErrorPage("405.html", "405 Method Not Allowed");
    }

    public HTTPResponse handle500(HTTPRequest req) {
        return loadErrorPage("500.html", "500 Internal Server Error");
    }

    private HTTPResponse loadErrorPage(String filename, String defaultMessage) {
        HTTPResponse res = new HTTPResponse();
        Path file = Path.of(errorDir, filename);
        if (Files.exists(file)) {
            try {
                String content = Files.readString(file);
                res.setBody(content);
                res.setStatus(Integer.parseInt(filename.split("\\.")[0]), defaultMessage);
            } catch (IOException ex) {
                res.setStatus(500, "Internal Server Error");
                res.setBody("Internal Server Error");
            }
        } else {

        }
        return res;
    }

}