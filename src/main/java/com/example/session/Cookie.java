package com.example.session;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class Cookie {
    private String name;
    private String value;
    private String domain;
    private String path;
    private Instant expires;
    private long maxAge;
    private boolean secure;
    private boolean httpOnly;
    private String sameSite;

    public Cookie(String name, String value) {
        this.name = name;
        this.value = value;
        this.path = "/";
        this.httpOnly = true; // Default to secure
        this.sameSite = "Lax";
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Instant getExpires() {
        return expires;
    }

    public void setExpires(Instant expires) {
        this.expires = expires;
    }

    public long getMaxAge() {
        return maxAge;
    }

    public void setMaxAge(long maxAge) {
        this.maxAge = maxAge;
    }

    public boolean isSecure() {
        return secure;
    }

    public void setSecure(boolean secure) {
        this.secure = secure;
    }

    public boolean isHttpOnly() {
        return httpOnly;
    }

    public void setHttpOnly(boolean httpOnly) {
        this.httpOnly = httpOnly;
    }

    public String getSameSite() {
        return sameSite;
    }

    public void setSameSite(String sameSite) {
        this.sameSite = sameSite;
    }

    public static Map<String, String> parseCookieHeader(String cookieHeader) {
        Map<String, String> cookies = new HashMap<>();
        if (cookieHeader == null || cookieHeader.isEmpty()) {
            return cookies;
        }

        String[] pairs = cookieHeader.split(";");
        for (String pair : pairs) {
            String[] parts = pair.trim().split("=", 2);
            if (parts.length == 2) {
                cookies.put(parts[0].trim(), parts[1].trim());
            }
        }
        return cookies;
    }

    public String toSetCookieHeader() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append("=").append(value);

        if (path != null) {
            sb.append("; Path=").append(path);
        }

        if (domain != null) {
            sb.append("; Domain=").append(domain);
        }

        if (maxAge > 0) {
            sb.append("; Max-Age=").append(maxAge);
        }

        if (expires != null) {
            DateTimeFormatter formatter = DateTimeFormatter
                    .ofPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'")
                    .withZone(ZoneId.of("GMT"));
            sb.append("; Expires=").append(formatter.format(expires));
        }

        if (secure) {
            sb.append("; Secure");
        }

        if (httpOnly) {
            sb.append("; HttpOnly");
        }

        if (sameSite != null) {
            sb.append("; SameSite=").append(sameSite);
        }

        return sb.toString();
    }

    public static Cookie createSessionCookie(String name, String value) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setSameSite("Lax");
        return cookie;
    }

    public static Cookie createPersistentCookie(String name, String value, long maxAgeSeconds) {
        Cookie cookie = new Cookie(name, value);
        cookie.setMaxAge(maxAgeSeconds);
        cookie.setHttpOnly(true);
        cookie.setSameSite("Lax");
        return cookie;
    }

    public static Cookie createDeleteCookie(String name) {
        Cookie cookie = new Cookie(name, "");
        cookie.setMaxAge(0);
        return cookie;
    }

    @Override
    public String toString() {
        return toSetCookieHeader();
    }
}