package com.example.core;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    private final int port;

    public Server(int port){
        this.port = port;
    }
    

    public void start(){
        try (ServerSocket serverSocket = new ServerSocket(port)){
            System.out.println("listening on : "+ port);
            
            while(true){                
                Socket client = serverSocket.accept();
                new ConnectionHandler(client).handle();
            }
        } catch(IOException ex){
            System.err.println("error:"+ ex.getMessage());
        }
    }
}