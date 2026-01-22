package com.example.config;

import org.json.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ConfigLoader {
    public static List<ServerConfig> load(String path) {
        List<ServerConfig> servers = new ArrayList<>();
        try {
            JSONObject json = new JSONObject(Files.readString(Path.of(path)));
            JSONArray arr = json.getJSONArray("servers");
            for (int i = 0; i < arr.length(); i++) {
                JSONObject s = arr.getJSONObject(i);
                ServerConfig cfg = new ServerConfig();

                cfg.host = s.getString("host");
                cfg.isDefault = s.getBoolean("isDefault");
                // cfg.root = s.getString("root");

                cfg.ports = new ArrayList<>();
                for (Object p : s.getJSONArray("ports")) {
                    cfg.ports.add((Integer) p);
                }
                servers.add(cfg);
            }
        } catch (IOException ex) {
            System.err.println("oh no: " + ex.getMessage());
        }
        return servers;
    }
}