package com.example;

import com.example.config.ConfigLoader;
import com.example.config.ServerConfig;
import com.example.core.Server;
import java.util.List;

public class App {
    public static void main(String[] args) {
        System.out.println("Starting server...");

        List<ServerConfig> servers = ConfigLoader.load("config.json");
        
        ServerConfig active = servers.stream()
        .filter(s -> s.isDefault)
        .findFirst()
        .orElseThrow();

        for (int port : active.ports){
            new Thread(() ->{
                new Server(port).start();
            }).start();
        }

    }

}
