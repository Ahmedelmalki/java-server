package com.example.routing;

import com.example.http.*;
import com.example.handlers.*;

public class Router {

    private final StaticFileHandler staticFileHandler = new StaticFileHandler();
    private final ErrorHandler errorHandler = new ErrorHandler();
    private final UploadHandler uploadHandler = new UploadHandler();
    private final CGIHandler cgiHandler = new CGIHandler();

    public HTTPResponse route(HTTPRequest req, String root){
        if(req.method.equals("GET")){
            return staticFileHandler.handle(req, root); // where should i get the root form
        } else{
            return errorHandler.handle405(req);
        }
    }

}