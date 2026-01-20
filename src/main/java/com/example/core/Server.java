package com.example.core;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import com.example.config.ServerConfig;

public class Server {

    private final ServerConfig config;

    public Server(ServerConfig config){
        this.config = config;
    }
    

    public void start(){
        try (ServerSocket serverSocket = new ServerSocket(config.port)){
            System.out.println("listening on : "+ config.port);
            
            while(true){                
                Socket client = serverSocket.accept();
                client.close();
            }
        } catch(IOException ex){
            System.err.println("error:"+ ex.getMessage());
        }
    }
}