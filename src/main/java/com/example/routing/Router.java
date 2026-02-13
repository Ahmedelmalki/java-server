package com.example.routing;

import java.util.List;
import java.util.Map;

import com.example.config.RouteConfig;
import com.example.config.ServerConfig;
import com.example.handlers.CGIHandler;
import com.example.handlers.Deletehandler;
import com.example.handlers.ErrorHandler;
import com.example.handlers.StaticFileHandler;
import com.example.handlers.UploadHandler;
import com.example.http.HTTPRequest;
import com.example.http.HTTPResponse;
import com.example.http.MultiPart;
import com.example.parser.MultipartParser;
import com.example.session.Cookie;
import com.example.session.Session;
import com.example.session.SessionManager;

public class Router {

    private final StaticFileHandler staticFileHandler = new StaticFileHandler();
    private final ErrorHandler errorHandler = new ErrorHandler();

    public HTTPResponse route(HTTPRequest req, ServerConfig serverConfig, byte[] body) {
        // ------- SESSION MANAGMENT -------
        String cookieHeader = req.headers.get("Cookie");
        Map<String, String> cookies = Cookie.parseCookieHeader(cookieHeader);
        SessionManager sm = SessionManager.getInstance();
        Session session = sm.getSession(cookies, true);
        // ------- END SESSION MANAGMENT -------

        // ===== PARSE MULTIPART IF PRESENT =====
        String contentType = req.headers.get("Content-Type");
        if (contentType != null && contentType.toLowerCase().startsWith("multipart/form-data")) {
            String boundary = extractBoundary(contentType);
            if (boundary != null && body != null && body.length > 0) {
                try {
                    MultipartParser parser = new MultipartParser(boundary);
                    List<MultiPart> parts = parser.parse(body);
                    req.setParts(parts);
                    System.out.println("Parsed " + parts.size() + " multipart parts");
                } catch (Exception e) {
                    System.err.println("Failed to parse multipart data: " + e.getMessage());
                }
            }
        }
        // ========================================

        RouteConfig matched = null;
        int longestMatch = -1;
        for (RouteConfig route : serverConfig.routes) {
            if (req.path.startsWith(route.path)) {
                if (route.path.length() > longestMatch) {
                    matched = route;
                    longestMatch = route.path.length();
                }
            }
        }

        HTTPResponse res = new HTTPResponse();

        if (matched == null) {
            res = errorHandler.handle404(req, serverConfig);
        } else if (matched.methods != null && !matched.methods.contains(req.method)) {
            res = errorHandler.handle405(req, serverConfig);
        } else if (matched.redirect != null) {
            res.setStatus(matched.redirect.code, "Redirect");
            res.addHeader("Location", matched.redirect.url);
        } else if (matched.cgi != null && !matched.cgi.isEmpty()) {
            res = CGIHandler.handle(req, matched);
        } else if (req.method.equals("DELETE")) {
            res = Deletehandler.handle(req, matched);
        } else if (matched.uploadEnabled && req.method.equals("POST")) {
            res = UploadHandler.handle(req, matched, body);
        } else {
            res = staticFileHandler.handle(req, matched, session);
            if (res.getStatusCode() == 404  ) {
                res = errorHandler.handle404(req, serverConfig);
            }
        }

        // session cookie if needed
        if (!cookies.containsKey("JSESSIONID")) {
            Cookie sessionCookie = sm.createSessionCookie(session);
            res.addCookie(sessionCookie);
            System.out.println("Sending new session cookie: " + session.getSessionId());
        }

        // ===== KEEP-ALIVE HEADERS if status is success=====
        if (res.getStatusCode() < 400 && "keep-alive".equalsIgnoreCase(req.headers.get("Connection"))) {
            res.addHeader("Connection", "keep-alive");
            res.addHeader("Keep-Alive", "timeout=30, max=100");
        } else {
            //in case of err 4.. 5.
            res.addHeader("Connection", "close");
        }
        // ==================================================

        return res;
    }

    // HELPER METHOD
    private String extractBoundary(String contentType) {
        for (String param : contentType.split(";")) {
            param = param.trim();
            if (param.toLowerCase().startsWith("boundary=")) {
                String boundary = param.substring(9);
                // Remove quotes if present
                if (boundary.startsWith("\"") && boundary.endsWith("\"")) {
                    boundary = boundary.substring(1, boundary.length() - 1);
                }
                return boundary;
            }
        }
        return null;
    }
}