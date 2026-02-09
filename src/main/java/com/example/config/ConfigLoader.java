package com.example.config;

import com.example.parser.ConfigParser;
import java.util.List;

public class ConfigLoader {
    
    public static List<ServerConfig> load(String path) {
        return ConfigParser.parse(path);
    }
}