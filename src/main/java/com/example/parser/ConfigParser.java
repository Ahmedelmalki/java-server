package com.example.parser;

import com.example.config.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigParser {
    
    public static List<ServerConfig> parse(String configPath) {
        List<ServerConfig> servers = new ArrayList<>();
        
        try {
            String content = Files.readString(Path.of(configPath));
            SimpleJsonParser parser = new SimpleJsonParser(content);
            Map<String, Object> root = (Map<String, Object>) parser.parse();
            
            List<Object> serversArray = (List<Object>) root.get("servers");
            
            for (Object serverObj : serversArray) {
                ServerConfig config = parseServerConfig((Map<String, Object>) serverObj);
                servers.add(config);
            }
            
        } catch (IOException ex) {
            throw new RuntimeException("Failed to load config file: " + configPath, ex);
        } catch (Exception ex) {
            throw new RuntimeException("Invalid JSON in config file: " + ex.getMessage(), ex);
        }
        
        return servers;
    }
    
    private static ServerConfig parseServerConfig(Map<String, Object> obj) {
        ServerConfig config = new ServerConfig();
        
        config.host = (String) obj.get("host");
        config.serverName = (String) obj.getOrDefault("serverName", null);
        config.isDefault = (Boolean) obj.get("isDefault");
        config.timeout = getLong(obj, "timeout", 30000L);
        config.clientMaxBodySize = getLong(obj, "clientMaxBodySize", 5242880L);
        
        // Parse error pages
        if (obj.containsKey("errorPages")) {
            config.errorPages = new HashMap<>();
            Map<String, Object> errorPagesObj = (Map<String, Object>) obj.get("errorPages");
            for (Map.Entry<String, Object> entry : errorPagesObj.entrySet()) {
                config.errorPages.put(entry.getKey(), (String) entry.getValue());
            }
        }
        
        // Parse ports
        config.ports = new ArrayList<>();
        List<Object> portsArray = (List<Object>) obj.get("ports");
        for (Object port : portsArray) {
            config.ports.add(((Number) port).intValue());
        }
        
        // Parse routes
        config.routes = new ArrayList<>();
        if (obj.containsKey("routes")) {
            List<Object> routesArray = (List<Object>) obj.get("routes");
            for (Object routeObj : routesArray) {
                RouteConfig route = parseRouteConfig((Map<String, Object>) routeObj);
                config.routes.add(route);
            }
        }
        
        return config;
    }
    
    private static RouteConfig parseRouteConfig(Map<String, Object> obj) {
        RouteConfig route = new RouteConfig();
        
        route.path = (String) obj.get("path");
        
        // Parse methods
        route.methods = new ArrayList<>();
        if (obj.containsKey("methods")) {
            List<Object> methodsArray = (List<Object>) obj.get("methods");
            for (Object method : methodsArray) {
                route.methods.add((String) method);
            }
        }
        
        route.root = (String) obj.getOrDefault("root", null);
        route.index = (String) obj.getOrDefault("index", null);
        route.autoindex = (Boolean) obj.getOrDefault("autoindex", false);
        route.uploadEnabled = (Boolean) obj.getOrDefault("uploadEnabled", false);
        
        // Parse CGI handlers
        if (obj.containsKey("cgi")) {
            route.cgi = new HashMap<>();
            Map<String, Object> cgiObj = (Map<String, Object>) obj.get("cgi");
            for (Map.Entry<String, Object> entry : cgiObj.entrySet()) {
                route.cgi.put(entry.getKey(), (String) entry.getValue());
            }
        }
        
        // Parse redirect
        if (obj.containsKey("redirect")) {
            Map<String, Object> redirectObj = (Map<String, Object>) obj.get("redirect");
            route.redirect = new RedirectConfig();
            route.redirect.code = ((Number) redirectObj.get("code")).intValue();
            route.redirect.url = (String) redirectObj.get("url");
        }
        
        return route;
    }
    
    private static long getLong(Map<String, Object> obj, String key, long defaultValue) {
        if (!obj.containsKey(key)) {
            return defaultValue;
        }
        Object value = obj.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return defaultValue;
    }
}
