package com.example.handlers;

import java.util.Map;
import com.example.http.*;
import com.example.session.*;

public class SessionHandler {
    private final SessionManager sessionManager = SessionManager.getInstance();

    public HTTPResponse handle(HTTPRequest req) {
        HTTPResponse res = new HTTPResponse();

        // Parse cookies from request
        String cookieHeader = req.headers.get("Cookie");
        Map<String, String> cookies = Cookie.parseCookieHeader(cookieHeader);

        // Get or create session
        Session session = sessionManager.getSession(cookies, true);

        // Track visit count
        Integer visitCount = session.getAttribute("visitCount", Integer.class);
        if (visitCount == null) {
            visitCount = 0;
        }
        visitCount++;
        session.setAttribute("visitCount", visitCount);

        // Store first visit time
        if (!session.hasAttribute("firstVisit")) {
            session.setAttribute("firstVisit", System.currentTimeMillis());
        }

        // Build response
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html>\n");
        html.append("<head><title>Session Demo</title></head>\n");
        html.append("<body>\n");
        html.append("<h1>Session Demo</h1>\n");
        html.append("<p>Session ID: ").append(session.getSessionId()).append("</p>\n");
        html.append("<p>Visit Count: ").append(visitCount).append("</p>\n");
        html.append("<p>Session Created: ").append(session.getCreatedAt()).append("</p>\n");
        html.append("<p>Last Accessed: ").append(session.getLastAccessedAt()).append("</p>\n");
        html.append("<p>Active Sessions: ").append(sessionManager.getActiveSessionCount()).append("</p>\n");
        html.append("<hr>\n");
        html.append("<form method='POST' action='/session-demo'>\n");
        html.append("<input type='submit' name='action' value='Refresh'/>\n");
        html.append("<input type='submit' name='action' value='Logout'/>\n");
        html.append("</form>\n");
        html.append("</body>\n");
        html.append("</html>\n");

        String body = html.toString();

        // Handle logout
        String action = req.headers.get("action");
        if ("Logout".equals(action)) {
            sessionManager.invalidateSession(session.getSessionId());
            res.addCookie(sessionManager.createDeleteSessionCookie());
            res.setStatus(302, "Found");
            res.addHeader("Location", "/session-demo");
            return res;
        }

        // Set session cookie if new session
        if (visitCount == 1) {
            Cookie sessionCookie = sessionManager.createSessionCookie(session);
            res.addCookie(sessionCookie);
        }

        res.setStatus(200, "OK");
        res.setBody(body);
        res.addHeader("Content-Type", "text/html; charset=utf-8");
        res.addHeader("Content-Length", String.valueOf(body.length()));

        return res;
    }

    /**
     * Example: Login handler that creates a session
     */
    public HTTPResponse handleLogin(HTTPRequest req, String username) {
        HTTPResponse res = new HTTPResponse();

        // Create new session
        Session session = sessionManager.createNewSession();
        session.setAttribute("username", username);
        session.setAttribute("loginTime", System.currentTimeMillis());
        session.setAttribute("authenticated", true);

        // Set session cookie
        Cookie sessionCookie = sessionManager.createSessionCookie(session);
        res.addCookie(sessionCookie);

        // Redirect to dashboard
        res.setStatus(302, "Found");
        res.addHeader("Location", "/dashboard");
        res.addHeader("Content-Length", "0");

        return res;
    }

    /**
     * Example: Logout handler that invalidates session
     */
    public HTTPResponse handleLogout(HTTPRequest req) {
        HTTPResponse res = new HTTPResponse();

        // Parse cookies and get session
        String cookieHeader = req.headers.get("Cookie");
        Map<String, String> cookies = Cookie.parseCookieHeader(cookieHeader);
        Session session = sessionManager.getSession(cookies, false);

        if (session != null) {
            sessionManager.invalidateSession(session.getSessionId());
        }

        // Delete session cookie
        res.addCookie(sessionManager.createDeleteSessionCookie());

        // Redirect to home
        res.setStatus(302, "Found");
        res.addHeader("Location", "/");
        res.addHeader("Content-Length", "0");

        return res;
    }

    /**
     * Example: Protected page that requires authentication
     */
    public HTTPResponse handleProtectedPage(HTTPRequest req) {
        HTTPResponse res = new HTTPResponse();

        // Parse cookies and get session
        String cookieHeader = req.headers.get("Cookie");
        Map<String, String> cookies = Cookie.parseCookieHeader(cookieHeader);
        Session session = sessionManager.getSession(cookies, false);

        // Check if authenticated
        if (session == null || !Boolean.TRUE.equals(session.getAttribute("authenticated", Boolean.class))) {
            res.setStatus(302, "Found");
            res.addHeader("Location", "/login");
            res.addHeader("Content-Length", "0");
            return res;
        }

        // User is authenticated, show protected content
        String username = session.getAttribute("username", String.class);
        String body = "<html><body><h1>Welcome, " + username + "!</h1></body></html>";

        res.setStatus(200, "OK");
        res.setBody(body);
        res.addHeader("Content-Type", "text/html");
        res.addHeader("Content-Length", String.valueOf(body.length()));

        return res;
    }

}