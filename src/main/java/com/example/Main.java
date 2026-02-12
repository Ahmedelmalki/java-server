package com.example;

import com.example.config.ConfigLoader;
import com.example.config.ServerConfig;
import com.example.core.Server;

import java.io.IOException;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        System.out.println("Starting server...");

        List<ServerConfig> servers = ConfigLoader.load("config.json");

        // ServerConfig active = servers.stream()
        // .filter(s -> s.isDefault)
        // .findFirst()
        // .orElseThrow();

        if (servers.isEmpty()) {
            System.err.println("No server configurations found!");
            return;
        }

        try {
            new Server(servers).start();
        } catch (IOException ex) {
            System.out.println("oh no: " + ex.getMessage());
        }
    }

}
