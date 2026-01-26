package com.example;

import com.example.config.ConfigLoader;
import com.example.config.RouteConfig;
import com.example.config.ServerConfig;
import com.example.core.Server;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        System.out.println("Starting server...");

        List<ServerConfig> servers = ConfigLoader.load("config.json");

        ServerConfig active = servers.stream()
                .filter(s -> s.isDefault)
                .findFirst()
                .orElseThrow();

        System.out.println("Active server routes: " + active.routes.size());
        for (RouteConfig r : active.routes) {
            System.out.println("  - " + r.path + " -> " + r.root);
        }
        try {
            new Server(active).start();
        } catch (IOException ex) {
            System.out.println("oh no: "+ex.getMessage());
        }
    }

}
