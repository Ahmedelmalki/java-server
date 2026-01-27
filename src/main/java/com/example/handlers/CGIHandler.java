package com.example.handlers;

import com.example.http.HTTPRequest;
import com.example.http.HTTPResponse;
import com.example.config.RouteConfig;
import java.nio.file.Path;

public class CGIHandler {

    public static HTTPResponse handle(HTTPRequest req, RouteConfig route) {
        HTTPResponse res = new HTTPResponse();

        try {
            String scriptPath = route.root + req.path.substring(route.path.length());
            System.out.println("route.root: " + route.root + "\n req.path.length(): " + route.path.length());
            System.out.println("script path: " + scriptPath);

            Path script = Path.of(scriptPath);
            String ext = getExt(script.toString());
            String interpreter = route.cgi.get(ext);

            if (interpreter == null) {
                res.setStatus(404, "Not Found");
                res.setBody("No CGI handler for " + ext);
                return res;
            }

 
            ProcessBuilder pb = new ProcessBuilder(interpreter, script.toString());
            Process p = pb.start();
            String output = new String(p.getInputStream().readAllBytes());

            res.setStatus(200, "OK");
            res.setBody(output);
            res.addHeader("Content-Type", "text/plain");
            res.addHeader("Content-Length", String.valueOf(output.length()));
            return res;
        } catch (Exception ex) {
            System.err.println("oh no: " + ex.getMessage());
            res.setStatus(500, "Internal Server Error");
            res.setBody("CGI Error");
            return res;
        }
    }

    private static String getExt(String path) {
        int i = path.lastIndexOf('.');
        return i == -1 ? "" : path.substring(i);
    }
}
