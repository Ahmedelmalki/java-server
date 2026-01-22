package com.example.handlers;

import com.example.http.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class StaticFileHandler {
    private final String root = "www/html"; // base folder

    public HTTPResponse handle(HTTPRequest req) {
        HTTPResponse res = new HTTPResponse();

        String safePath = req.path.equals("/") ? "/index.html" : req.path;
        Path file = Path.of(root + safePath).normalize();

        try {
            if (!Files.exists(file) || Files.isDirectory(file)) {
                res.setStatus(404, "Not Found");
                res.setBody("Not Found");
            } else {
                byte[] content = Files.readAllBytes(file);
                res.setBody(new String(content));
            }

        } catch (IOException ex) {
            res.setStatus(500, "Internal Server Error");
            res.setBody("Internal Server Error");
        }
        return res;
    }
}