package com.example.routing;

import com.example.http.*;
import com.example.config.RouteConfig;
import com.example.config.ServerConfig;
import com.example.handlers.*;

public class Router {

    private final StaticFileHandler staticFileHandler = new StaticFileHandler();
    private final ErrorHandler errorHandler = new ErrorHandler();
    private final UploadHandler uploadHandler = new UploadHandler();

    public HTTPResponse route(HTTPRequest req, ServerConfig serverConfig) {

        RouteConfig matched = null;
        System.out.println("req.path: " + req.path);
        for (RouteConfig route : serverConfig.routes) {
            if (req.path.startsWith(route.path)) {
                matched = route;
                break;
            }
        }

        if (matched == null) {
            return errorHandler.handle404(req);
        }
        if (matched.redirect != null) {
            HTTPResponse res = new HTTPResponse();
            res.setStatus(matched.redirect.code, "Redirect");
            res.addHeader("Location", matched.redirect.url);
            return res;
        }

        if (matched.methods != null && !matched.methods.contains(req.method)) {
            return errorHandler.handle405(req);
        }
        if (matched.cgi != null && !matched.cgi.isEmpty()) {
            return CGIHandler.handle(req, matched);
        }

        // if(matched.uploadEnabled && req.method.equals("POST")){
        // rn uploadHandler.handle(req, matched);
        // }

        return staticFileHandler.handle(req, matched);
    }

}