package com.example.config;

import org.json.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ConfigLoader {
    public static List<ServerConfig> load(String path) {
        // System.out.println("##### Loading config from: " +
        // Path.of(path).toAbsolutePath());

        List<ServerConfig> servers = new ArrayList<>();
        try {
            JSONObject json = new JSONObject(Files.readString(Path.of(path)));
            JSONArray arr = json.getJSONArray("servers");
            for (int i = 0; i < arr.length(); i++) {

                JSONObject s = arr.getJSONObject(i);
                ServerConfig cfg = new ServerConfig();

                cfg.host = s.getString("host");
                cfg.isDefault = s.getBoolean("isDefault");
                cfg.timeout = s.optLong("timeout", 30000);
                System.out.println("$$$ timeout : " + cfg.timeout);

                cfg.ports = new ArrayList<>();
                for (Object p : s.getJSONArray("ports")) {
                    cfg.ports.add(((Number) p).intValue());
                }

                cfg.routes = new ArrayList<>();
                JSONArray routesArray = s.getJSONArray("routes");
                for (int j = 0; j < routesArray.length(); j++) {

                    JSONObject r = routesArray.getJSONObject(j);
                    RouteConfig route = new RouteConfig();

                    route.path = r.getString("path");
                    // System.out.println("Loaded route: path=" + route.path + ", root=" +
                    // route.root);

                    route.methods = new ArrayList<>();
                    if (r.has("methods")) {
                        JSONArray methodArray = r.getJSONArray("methods");
                        for (int k = 0; k < methodArray.length(); k++) {
                            route.methods.add(methodArray.getString(k));
                        }
                    }

                    route.root = r.optString("root", null);
                    route.index = r.optString("index", null);
                    route.autoindex = r.optBoolean("autoindex", false);
                    route.uploadEnabled = r.optBoolean("uploadEnabled", false);

                    if (r.has("cgi")) {
                        route.cgi = new HashMap<>();
                        JSONObject cjiObj = r.getJSONObject("cgi");
                        for (String key : cjiObj.keySet()) {
                            route.cgi.put(key, cjiObj.getString(key));
                        }
                    }

                    if (r.has("redirect")) {
                        JSONObject rdObj = r.getJSONObject("redirect");
                        route.redirect = new RedirectConfig();
                        route.redirect.code = rdObj.getInt("code");
                        route.redirect.url = rdObj.getString("url");
                    }

                    cfg.routes.add(route);
                }
                servers.add(cfg);
            }
        } catch (IOException ex) {
            System.err.println("oh no: " + ex.getMessage());
        }
        return servers;
    }
}
