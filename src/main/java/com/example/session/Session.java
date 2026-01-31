package com.example.session;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Session {
    private final String sessionId;
    private final Map<String, Object> attributes;
    private Instant createdAt;
    private Instant lastAccessedAt;
    private long maxInactiveInterval;

    public Session(String sessionId) {
        this.sessionId = sessionId;
        this.attributes = new HashMap<>();
        this.createdAt = Instant.now();
        this.lastAccessedAt = Instant.now();
        this.maxInactiveInterval = 1800; // 30 minutes default
    }

    public static Session create() {
        return new Session(UUID.randomUUID().toString());
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setAttribute(String name, Object value) {
        attributes.put(name, value);
        updateLastAccessTime();
    }

    public Object getAttribute(String name) {
        updateLastAccessTime();
        return attributes.get(name);
    }

    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String name, Class<T> type) {
        Object value = getAttribute(name);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }

    public void removeAttribute(String name) {
        attributes.remove(name);
        updateLastAccessTime();
    }

    public boolean hasAttribute(String name) {
        return attributes.containsKey(name);
    }

    public Iterable<String> getAttributeNames() {
        return attributes.keySet();
    }

    public void invalidate() {
        attributes.clear();
    }

    public boolean isExpired() {
        long s = Instant.now().getEpochSecond() - lastAccessedAt.getEpochSecond(); // seconds Since Last Access
        return s > maxInactiveInterval;
    }

    public void updateLastAccessTime() {
        this.lastAccessedAt = Instant.now();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastAccessedAt() {
        return lastAccessedAt;
    }

    public void setMaxInactiveInterval(long seconds) {
        this.maxInactiveInterval = seconds;
    }

    public long getMaxInactiveInterval() {
        return maxInactiveInterval;
    }

    public int getAttributeCount() {
        return attributes.size();
    }

    @Override
    public String toString() {
        return String.format("Session{id='%s', attributes=%d, created=%s, lastAccessed=%s}",
                sessionId, attributes.size(), createdAt, lastAccessedAt);
    }
}