package com.example.handlers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import com.example.config.RouteConfig;
import com.example.http.HTTPRequest;
import com.example.http.HTTPResponse;
import com.example.session.Session;

public class StaticFileHandler {

    public HTTPResponse handle(HTTPRequest req, RouteConfig route, Session session) {
        HTTPResponse res = new HTTPResponse();

        try {
            if (session != null) {
                session.setAttribute("lastFile", req.path);
            }

            String relPath = req.path.substring(route.path.length());

            Path root = Path.of(route.root).toAbsolutePath().normalize();
            Path file = root.resolve(relPath.startsWith("/") ? relPath.substring(1) : relPath).normalize();

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
                // check for index file
                String indexFileName = (route.index != null) ? route.index : "index.html";
                Path indexFile = file.resolve(indexFileName);

                if (Files.exists(indexFile) && !Files.isDirectory(indexFile)) {
                    //index exist
                    file = indexFile;
                } else if (route.autoindex) {
                    //autoindex on but no index file
                    return generateDirectoryListing(file, root, req.path, route);
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

    private HTTPResponse generateDirectoryListing(Path directory, Path root, String requestPath, RouteConfig route) {
        HTTPResponse res = new HTTPResponse();
        try {
            final String finalRequestPath = requestPath.endsWith("/") ? requestPath : requestPath + "/";

            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html>\n");
            html.append("<html lang=\"en\">\n");
            html.append("<head>\n");
            html.append("    <meta charset=\"UTF-8\">\n");
            html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
            html.append("    <title>Index of ").append(finalRequestPath).append("</title>\n");
            html.append("    <style>\n");
            html.append("      body { font-family: Arial, sans-serif; margin: 40px; }\n");
            html.append("      h1 { border-bottom: 1px solid #ccc; padding-bottom: 10px; }\n");
            html.append("      table { border-collapse: collapse; width: 100%; }\n");
            html.append(
                    "      th { text-align: left; padding: 10px; background-color: #f0f0f0; border-bottom: 2px solid #ddd; }\n");
            html.append("      td { padding: 8px; border-bottom: 1px solid #eee; }\n");
            html.append("      a { text-decoration: none; color: #0066cc; }\n");
            html.append("      a:hover { text-decoration: underline; }\n");
            html.append("      .dir { font-weight: bold; }\n");
            html.append("      .size { text-align: right; }\n");
            html.append("      .date { color: #666; }\n");
            html.append("    </style>\n");
            html.append("</head>\n");
            html.append("<body>\n");
            html.append("    <h1>Index of ").append(escapeHtml(finalRequestPath)).append("</h1>\n");
            html.append("    <table>\n");
            html.append("        <thead>\n");
            html.append("            <tr>\n");
            html.append("                <th>Name</th>\n");
            html.append("                <th>Size</th>\n");
            html.append("                <th>Last Modified</th>\n");
            html.append("            </tr>\n");
            html.append("        </thead>\n");
            html.append("        <tbody>\n");

            if (!directory.equals(root)) {
                String parentPath = finalRequestPath.substring(0,
                        finalRequestPath.lastIndexOf('/', finalRequestPath.length() - 2) + 1);
                html.append("            <tr>\n");
                html.append("                <td class=\"dir\"><a href=\"").append(parentPath)
                        .append("\">../</a></td>\n");
                html.append("                <td class=\"size\">-</td>\n");
                html.append("                <td class=\"date\">-</td>\n");
                html.append("            </tr>\n");
            }
            try (Stream<Path> paths = Files.list(directory)) {
                paths.sorted((p1, p2) -> {
                    // Directories first, then files, alphabetically
                    boolean isDir1 = Files.isDirectory(p1);
                    boolean isDir2 = Files.isDirectory(p2);
                    if (isDir1 && !isDir2)
                        return -1;
                    if (!isDir1 && isDir2)
                        return 1;
                    return p1.getFileName().toString().compareToIgnoreCase(p2.getFileName().toString());
                }).forEach(path -> {
                    try {
                        String name = path.getFileName().toString();
                        boolean isDir = Files.isDirectory(path);
                        String displayName = isDir ? name + "/" : name;
                        String href = finalRequestPath + name + (isDir ? "/" : "");

                        long size = isDir ? 0 : Files.size(path);
                        String sizeStr = isDir ? "-" : formatSize(size);

                        String modified = Files.getLastModifiedTime(path).toString().substring(0, 19).replace('T', ' ');

                        html.append("            <tr>\n");
                        html.append("                <td");
                        if (isDir)
                            html.append(" class=\"dir\"");
                        html.append("><a href=\"").append(href).append("\">")
                                .append(escapeHtml(displayName)).append("</a></td>\n");
                        html.append("                <td class=\"size\">").append(sizeStr).append("</td>\n");
                        html.append("                <td class=\"date\">").append(modified).append("</td>\n");
                        html.append("            </tr>\n");
                    } catch (IOException e) {
                        // Skip files we can't read
                    }
                });
            }

            html.append("        </tbody>\n");
            html.append("    </table>\n");
            html.append("</body>\n");
            html.append("</html>\n");

            res.setStatus(200, "OK");
            res.setBody(html.toString());
            res.addHeader("Content-Type", "text/html; charset=utf-8");
            res.addHeader("Content-Length", String.valueOf(html.toString().getBytes().length));

            return res;
        } catch (IOException ex) {
            System.err.println("oh no: " + ex.getMessage());
            res.setStatus(500, "Internal Server Error");
            res.setBody("Error generating directory listing");
            return res;
        }
    }

    private String formatSize(long bytes) {
        if (bytes < 1024)
            return bytes + " B";
        if (bytes < 1024 * 1024)
            return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024)
            return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    private String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
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
