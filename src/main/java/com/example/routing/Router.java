package com.example.routing;

import com.example.http.*;
import com.example.handlers.*;

public class Router {

    private final StaticFileHandler staticFileHandler = new StaticFileHandler();
    private final ErrorHandler errorHandler = new ErrorHandler();

    //TODO:
    public HTTPResponse route(HTTPRequest req){
        if(req.method.equals("GET")){
            return staticFileHandler.handle(req);
        } else{
            return errorHandler.handle405(req);
        }
    }

}