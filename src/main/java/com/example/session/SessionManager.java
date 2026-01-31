package com.example.session;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {
    private static final String SESSION_COOKIE_NAME = "JSESSIONID";
    private static SessionManager instance;

    private final Map<String, Session> sessions;
    private long defaultMaxInactiveInterval;
    private long lastCleanupTime;

    private SessionManager() {
        System.out.println("@@@ entered SessionManager()");
        this.sessions = new ConcurrentHashMap<>();
        this.defaultMaxInactiveInterval = 1800;
        this.lastCleanupTime = System.currentTimeMillis();
    }

    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public Session getSession(Map<String, String> cookies, boolean create) {
        String sessionId = cookies.get(SESSION_COOKIE_NAME);

        if (sessionId != null && sessions.containsKey(sessionId)) {
            Session session = sessions.get(sessionId);

            if (session.isExpired()) {
                sessions.remove(sessionId);
                if (create) {
                    return createNewSession();
                }
                return null;
            }

            session.updateLastAccessTime();
            return session;
        }

        if (create) {
            return createNewSession();
        }
        return null;
    }

    public Session createNewSession() {
        Session session = Session.create();
        session.setMaxInactiveInterval(defaultMaxInactiveInterval);
        sessions.put(session.getSessionId(), session);
        System.out.println("Created new session: " + session.getSessionId());
        return session;
    }

    public Session getSession(String sessionId) {
        Session session = sessions.get(sessionId);
        if (session != null && !session.isExpired()) {
            session.updateLastAccessTime();
            return session;
        }
        return null;
    }

    public void removeSession(String sessionId) {
        sessions.remove(sessionId);
        System.out.println("Removed session: " + sessionId);
    }

    public void invalidateSession(String sessionId) {
        Session session = sessions.get(sessionId);
        if (session != null) {
            session.invalidate();
            sessions.remove(sessionId);
            System.out.println("Invalidated session: " + sessionId);
        }
    }

    public int getActiveSessionCount() {
        return sessions.size();
    }

    public void setDefaultMaxInactiveInterval(long seconds) {
        this.defaultMaxInactiveInterval = seconds;
    }

    public long getDefaultMaxInactiveInterval() {
        return defaultMaxInactiveInterval;
    }

    public Cookie createSessionCookie(Session session) {
        Cookie cookie = Cookie.createSessionCookie(SESSION_COOKIE_NAME, session.getSessionId());
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSameSite("Lax");
        return cookie;
    }

    public Cookie createDeleteSessionCookie() {
        return Cookie.createDeleteCookie(SESSION_COOKIE_NAME);
    }

    public void cleanupExpiredSessions() {
        long currentTime = System.currentTimeMillis();

        // Only run cleanup every 5 minutes to avoid excessive overhead
        if (currentTime - lastCleanupTime < 300000) {
            return;
        }

        lastCleanupTime = currentTime;

        int removed = 0;
        for (Map.Entry<String, Session> e : sessions.entrySet()) {
            if (e.getValue().isExpired()) {
                sessions.remove(e.getKey());
                removed++;
            }
        }

        if (removed > 0) {
            System.out.println("Cleaned up " + removed + " expired sessions. Active sessions: " + sessions.size());
        }
    }

    public static String getSessionCookieName() {
        return SESSION_COOKIE_NAME;
    }
}