package com.example.core;

import java.net.Socket;
import java.io.*;
import com.example.http.HTTPRequest;
import com.example.http.HTTPResponse;

public class ConnectionHandler {

    private final Socket socket;

    public ConnectionHandler(Socket socket) {
        this.socket = socket;
    }

    public void handle() {
        try (InputStream in = socket.getInputStream();
                OutputStream out = socket.getOutputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));) {

            HTTPRequest req = HTTPRequest.parse(reader);
            if (req == null)
                return;
            System.out.println("Received: " + req.method + " " + req.path + " " + req.headers.toString());

            HTTPResponse res = new HTTPResponse();
            res.setBody("welcome to hell");
            
            out.write(res.toBytes());
            out.flush();

        } catch (IOException ex) {
            System.err.println("oh no: " + ex.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    /*
     * handle(socket):
     * input = socket.getInputStream()
     * output = socket.getOutputStream()
     * 
     * request = parseHTTPRequest(input)
     * response = route(request)
     * writeHTTPResponse(output, response)
     * 
     * close socket
     * 
     */
}