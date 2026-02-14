package com.example.handlers;

import java.io.IOException;

import com.example.config.RouteConfig;
import com.example.http.HTTPRequest;
import com.example.http.HTTPResponse;
import com.example.http.MultiPart;  

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;  
import java.util.concurrent.CountDownLatch;

public class UploadHandler {

    public static HTTPResponse handle(HTTPRequest req, RouteConfig matched, byte[] body) {

        HTTPResponse res = new HTTPResponse();

        try {
            if (!matched.uploadEnabled) {
                res.setStatus(403, "Forbidden");
                res.setBody("Uploads not allowed here");
                return res;
            }

            Path uploadRoot = Path.of(matched.root).toAbsolutePath().normalize();
            Files.createDirectories(uploadRoot);

            // ===== HANDLE MULTIPART UPLOADS  
            if (req.hasMultipartData()) {
                List<MultiPart> parts = req.getParts();
                StringBuilder result = new StringBuilder();
                result.append("Uploaded ").append(parts.size()).append(" file(s):\n");

                int counter = 0;
                for (MultiPart part : parts) {
                    if (part.isFile()) {
                        String originalFilename = part.getFilename();
                        String filename;

                        if (originalFilename != null && !originalFilename.isBlank()) { // remove ../ path traversal
                            filename = Path.of(originalFilename).getFileName().toString();

                            if (parts.size() > 1) {
                                filename = counter + "_" + filename;
                            }

                        } else {
                            filename = "upload_" + Instant.now().toEpochMilli()+"_" + counter;
                        }

                        // String extension = "";
                        // int dotIndex = originalFilename.lastIndexOf('.');
                        // if (dotIndex > 0) {
                        //     extension = originalFilename.substring(dotIndex);
                        // }

                         // filename = "upload_" + Instant.now().toEpochMilli() + extension;
                        Path outFile = uploadRoot.resolve(filename);

                        Files.write(outFile, part.getData());
                        result.append("  - ").append(filename)
                                .append(" (").append(part.getData().length).append(" bytes)\n");
                    }
                }

                res.setStatus(201, "Created");
                res.setBody(result.toString());
                res.addHeader("Content-Type", "text/plain");
                return res;
            }
            // =====================================

            // Handle binary upload (existing code)
            if (body == null || body.length == 0) {
                res.setStatus(400, "Bad Request");
                res.setBody("Empty upload");
                return res;
            }

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
