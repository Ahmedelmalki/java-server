package com.example;

import com.example.config.ServerConfig;
import com.example.core.Server;

public class App {
    public static void main(String[] args) {
        System.out.println("Starting server...");

        // 1. Load config
        ServerConfig config = loadConfig("config.json");

        // 2. Start server
        Server server = new Server(config);
        server.start();
    }

    // Placeholder for now
    static ServerConfig loadConfig(String path) {
        // TODO: implement JSON parsing
        return new ServerConfig();
    }
}
