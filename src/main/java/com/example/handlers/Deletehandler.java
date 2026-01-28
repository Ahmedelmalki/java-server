package com.example.handlers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import com.example.config.RouteConfig;
import com.example.http.HTTPRequest;
import com.example.http.HTTPResponse;

public class Deletehandler {

    public static HTTPResponse handle(HTTPRequest req, RouteConfig matched) {
        HTTPResponse res = new HTTPResponse();

        try {
            if (matched.methods == null || !matched.methods.contains("DELETE")) {
                res.setStatus(405, "Method Not Allowed");
                res.setBody("DELETE not allowed on this route");
                res.addHeader("Content-Type", "text/plain");
                return res;
            }
            String relPath = req.path.substring(matched.path.length());
            System.out.println("req.path: " + req.path + " relPath: " + relPath);

            if (relPath.isEmpty() || relPath.equals("/")) {
                res.setStatus(400, "Bad Request");
                res.setBody("No file specified for deletion");
                res.addHeader("Content-Type", "text/plain");
                return res;
            }

            Path root = Path.of(matched.root).toAbsolutePath().normalize(); // but why thu
            Path fileToDelete = root.resolve(relPath.startsWith("/") ? relPath.substring(1) : relPath).normalize();

            if (!fileToDelete.startsWith(root)) {
                res.setStatus(403, "Forbidden");
                res.setBody("Access denied");
                res.addHeader("Content-Type", "text/plain");
                return res;
            }

            if (!Files.exists(fileToDelete)) {
                res.setStatus(404, "Not Found");
                res.setBody("File not found");
                res.addHeader("Content-Type", "text/plain");
                return res;
            }

            if (Files.isDirectory(fileToDelete)) {
                res.setStatus(403, "Forbidden");
                res.setBody("Cannot delete directories");
                res.addHeader("Content-Type", "text/plain");
                return res;
            }

            Files.delete(fileToDelete);

            res.setStatus(200, "OK");
            res.setBody("File deleted successfully: " + relPath);
            res.addHeader("Content-Type", "text/plain");

            System.out.println("Deleted file: " + fileToDelete);

            return res;

        } catch (IOException ex) {
            System.err.println("Error deleting file: " + ex.getMessage());
            res.setStatus(500, "Internal Server Error");
            res.setBody("Failed to delete file");
            res.addHeader("Content-Type", "text/plain");
            return res;
        }
    }
}