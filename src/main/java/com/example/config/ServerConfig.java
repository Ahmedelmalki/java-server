package com.example.config;

import java.util.List;
import java.util.Map;

public class ServerConfig {
    public String host;
    public String serverName;
    public List<Integer> ports;
    public boolean isDefault;
    public List<RouteConfig> routes;
    public long timeout;
    public long clientMaxBodySize;
    public Map<String, String> errorPages;
}
