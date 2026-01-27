package com.example.handlers;

import java.io.IOException;
import com.example.config.RouteConfig;
import com.example.http.HTTPRequest;
import com.example.http.HTTPResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

public class UploadHandler {

    public static HTTPResponse handle(HTTPRequest req, RouteConfig matched, byte[] body) {
        
        HTTPResponse res = new HTTPResponse();

        try {
            if (!matched.uploadEnabled) {
                res.setStatus(403, "Forbidden");
                res.setBody("Uploads not allowed here");
                return res;
            }
            if (body == null || body.length == 0) {
                res.setStatus(400, "Bad Request");
                res.setBody("Empty upload");
                return res;
            }

            Path uploadRoot = Path.of(matched.root).toAbsolutePath().normalize();
            Files.createDirectories(uploadRoot);

            String filename = "upload_" + Instant.now().toEpochMilli() + ".bin";
            Path outFile = uploadRoot.resolve(filename);

            Files.write(outFile, body);

            res.setStatus(201, "Created");
            res.setBody("Uploaded as: " + filename);
            res.addHeader("Content-Type", "text/plain");

            return res;
        } catch (IOException ex) {
            System.err.println("oh no: " + ex.getMessage());
            res.setStatus(500, "Internal Server Error");
            res.setBody("Upload failed");
            return res;
        }
    }
}
