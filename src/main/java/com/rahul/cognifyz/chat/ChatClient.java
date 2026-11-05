package com.rahul.cognifyz.chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/** TCP client that sends chat lines and receives server messages. */
public final class ChatClient implements AutoCloseable {
    private final Socket socket;
    private final BufferedReader reader;
    private final PrintWriter writer;
    private ChatMessage greeting;
    private volatile boolean closed;

    public ChatClient(String host,int port,String username)throws IOException{
        if(host==null||host.trim().isEmpty())throw new IllegalArgumentException("Server host is required.");
        if(port<1||port>65535)throw new IllegalArgumentException("Port must be between 1 and 65535.");
        if(!UserManager.isValidUsername(username))throw new IllegalArgumentException("Username must be 3-16 characters using letters, digits, or underscore.");
        Socket connected=new Socket();
        BufferedReader connectedReader=null;PrintWriter connectedWriter=null;ChatMessage welcome;
        try{
            connected.connect(new java.net.InetSocketAddress(host,port),5000);connected.setSoTimeout(5000);
            connectedReader=new BufferedReader(new InputStreamReader(connected.getInputStream(),StandardCharsets.UTF_8));
            connectedWriter=new PrintWriter(new OutputStreamWriter(connected.getOutputStream(),StandardCharsets.UTF_8),true);
            connectedWriter.println(username);
            if(connectedWriter.checkError())throw new IOException("Could not send username to chat server.");
            String response=connectedReader.readLine();
            if(response==null)throw new IOException("Chat server closed the connection during sign-in.");
            try{welcome=ChatMessage.parse(response);}catch(IllegalArgumentException ex){throw new IOException("Invalid response from chat server.");}
            if(welcome.getType()==ChatMessage.Type.ERROR)throw new IOException(welcome.getText());
            if(welcome.getType()!=ChatMessage.Type.INFO)throw new IOException("Unexpected response from chat server.");
            connected.setSoTimeout(0);
        }catch(IOException|RuntimeException ex){
            try{connected.close();}catch(IOException ignored){}
            throw ex;
        }
        socket=connected;reader=connectedReader;writer=connectedWriter;greeting=welcome;
    }
    public ChatMessage getGreeting(){return greeting;}
    public synchronized void sendMessage(String line)throws IOException{
        if(closed)throw new IOException("Chat client is disconnected.");
        if(line==null||line.indexOf('\n')>=0||line.indexOf('\r')>=0)throw new IllegalArgumentException("A chat message must be one line.");
        writer.println(line);if(writer.checkError())throw new IOException("Could not send message to chat server.");
    }
    public ChatMessage readMessage()throws IOException{
        String line=reader.readLine();if(line==null)return null;
        try{return ChatMessage.parse(line);}catch(IllegalArgumentException ex){throw new IOException("Received malformed chat message.");}
    }
    public void setReadTimeout(int milliseconds)throws IOException{
        if(milliseconds<0)throw new IllegalArgumentException("Read timeout cannot be negative.");
        socket.setSoTimeout(milliseconds);
    }
    public void runInteractive(BufferedReader consoleInput,PrintStream output)throws IOException{
        if(consoleInput==null||output==null)throw new IllegalArgumentException("Input and output are required.");
        final Thread receiver=new Thread(new Runnable(){@Override public void run(){
            try{ChatMessage message;while((message=readMessage())!=null)output.println(message.toDisplayString());}
            catch(IOException ex){if(!closed)output.println("Connection to chat server was lost.");}
        }},"chat-client-receiver");
        receiver.setDaemon(true);receiver.start();
        try{
            String line;
            while((line=consoleInput.readLine())!=null){sendMessage(line);if("/quit".equals(line))break;}
        }finally{close();try{receiver.join(1000);}catch(InterruptedException ex){Thread.currentThread().interrupt();}}
    }
    @Override public synchronized void close()throws IOException{
        if(closed)return;closed=true;
        try{socket.close();}finally{try{reader.close();}finally{writer.close();}}
    }
}
