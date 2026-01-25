package com.example.config;

import java.util.List;
import java.util.Map;

public class RouteConfig {
    public String path;
    public List<String> methods;
    public String root;
    public String index;
    public boolean autoindex;
    public boolean uploadEnabled;
    public Map<String, String> cgi;
    public RedirectConfig redirect;
}