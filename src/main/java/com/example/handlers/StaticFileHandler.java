package com.example.handlers;

import com.example.http.HTTPRequest;
import com.example.http.HTTPResponse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class StaticFileHandler {

    public HTTPResponse handle(HTTPRequest req, String rootDir) {
        HTTPResponse res = new HTTPResponse();

        try {
            // Default file
            String relPath = req.path.equals("/") ? "/index.html" : req.path;

            Path root = Path.of(rootDir).toAbsolutePath().normalize();
            Path file = root.resolve(relPath.substring(1)).normalize();

            // Directory traversal protection
            if (!file.startsWith(root)) {
                res.setStatus(403, "Forbidden");
                res.setBody("Forbidden");
                return res;
            }

            if (!Files.exists(file)) {
                res.setStatus(404, "Not Found");
                res.setBody("Not Found");
                return res;
            }

            if (Files.isDirectory(file)) {
                // try index.html inside dir
                Path index = file.resolve("index.html");
                if (Files.exists(index)) {
                    file = index;
                } else {
                    res.setStatus(403, "Forbidden");
                    res.setBody("Directory listing disabled");
                    return res;
                }
            }

            byte[] content = Files.readAllBytes(file);

            res.setStatus(200, "OK");
            res.setBodyBytes(content);
            res.addHeader("Content-Type", guessContentType(file));
            res.addHeader("Content-Length", String.valueOf(content.length));

            return res;

        } catch (IOException e) {
            res.setStatus(500, "Internal Server Error");
            res.setBody("Internal Server Error");
            return res;
        }
    }

    private String guessContentType(Path file) {
        String name = file.getFileName().toString().toLowerCase();
        if (name.endsWith(".html"))
            return "text/html";
        if (name.endsWith(".css"))
            return "text/css";
        if (name.endsWith(".js"))
            return "application/javascript";
        if (name.endsWith(".png"))
            return "image/png";
        if (name.endsWith(".jpg") || name.endsWith(".jpeg"))
            return "image/jpeg";
        if (name.endsWith(".gif"))
            return "image/gif";
        if (name.endsWith(".txt"))
            return "text/plain";
        return "application/octet-stream";
    }
}
