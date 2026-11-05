package com.rahul.cognifyz.chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Owns one client socket and processes its incoming lines on a dedicated thread. */
public final class ClientHandler implements Runnable,ChatSession {
    private static final Logger LOGGER=Logger.getLogger(ClientHandler.class.getName());
    private final Socket socket;
    private final UserManager users;
    private final MessageHandler messages;
    private final Consumer<ClientHandler> closeListener;
    private final BufferedReader reader;
    private final PrintWriter writer;
    private final Object writeLock=new Object();
    private volatile String username;
    private volatile boolean registered;
    private volatile boolean closed;

    ClientHandler(Socket socket,UserManager users,MessageHandler messages,Consumer<ClientHandler> closeListener)throws IOException{
        this.socket=socket;this.users=users;this.messages=messages;this.closeListener=closeListener;
        reader=new BufferedReader(new InputStreamReader(socket.getInputStream(),StandardCharsets.UTF_8));
        writer=new PrintWriter(new OutputStreamWriter(socket.getOutputStream(),StandardCharsets.UTF_8),true);
    }
    void setUsername(String value){username=value;}
    @Override public String getUsername(){return username;}
    @Override public void run(){
        try{
            socket.setTcpNoDelay(true);
            String requested=reader.readLine();
            if(requested==null){send(ChatMessage.error("Username is required."));return;}
            requested=requested.trim();setUsername(requested);
            if(!UserManager.isValidUsername(requested)){send(ChatMessage.error("Username must be 3-16 characters using letters, digits, or underscore."));return;}
            if(!users.register(requested,this)){send(ChatMessage.error("Username is already in use."));return;}
            registered=true;
            send(ChatMessage.info("Welcome, "+requested+". Use /help to see commands."));
            LOGGER.info("Client joined: "+requested);
            users.broadcast(ChatMessage.info(requested+" joined the chat."),null);
            String line;
            while(!closed&&(line=reader.readLine())!=null)if(!messages.handle(this,line))break;
        }catch(IOException ex){if(!closed)LOGGER.log(Level.FINE,"Client connection ended unexpectedly.");}
        finally{
            if(registered&&users.remove(username,this)){
                LOGGER.info("Client disconnected: "+username);
                users.broadcast(ChatMessage.info(username+" left the chat."),this);
            }
            try{close();}catch(IOException ignored){}
            if(closeListener!=null)closeListener.accept(this);
        }
    }
    @Override public void send(ChatMessage message)throws IOException{
        synchronized(writeLock){if(closed)throw new IOException("Client is disconnected.");writer.println(message.format());if(writer.checkError())throw new IOException("Failed to write to client.");}
    }
    @Override public synchronized void close()throws IOException{
        if(closed)return;closed=true;
        try{socket.close();}finally{try{reader.close();}finally{writer.close();}}
    }
}
