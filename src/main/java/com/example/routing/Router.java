package com.example.routing;

import com.example.http.*;
import com.example.session.*;
import com.example.config.*;
import com.example.handlers.*;
import java.util.Map;

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
        }

        // session cookie if needed
        if (!cookies.containsKey("JSESSIONID")) {
            Cookie sessionCookie = sm.createSessionCookie(session);
            res.addCookie(sessionCookie);
            System.out.println("Sending new session cookie: " + session.getSessionId());
        }

        // ===== KEEP-ALIVE HEADERS =====
        if ("keep-alive".equalsIgnoreCase(req.headers.get("Connection"))) {
            res.addHeader("Connection", "keep-alive");
            res.addHeader("Keep-Alive", "timeout=30, max=100");
        } else {
            res.addHeader("Connection", "close");
        }
        // ==================================================

        return res;
    }
}