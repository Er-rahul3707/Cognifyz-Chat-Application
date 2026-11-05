package com.rahul.cognifyz.chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Command-line entry point. Use arguments or the interactive role prompt. */
public final class Main {
    private Main() { }
    public static void main(String[] args){
        BufferedReader console=new BufferedReader(new InputStreamReader(System.in,StandardCharsets.UTF_8));
        InputHandler input=new InputHandler(console);
        try{
            String mode=args.length>0?args[0]:input.readLine("Start as server or client? (server/client): ");
            if("server".equalsIgnoreCase(mode))runServer(args,input,console);
            else if("client".equalsIgnoreCase(mode))runClient(args,input,console);
            else System.out.println("Choose server or client to start.");
        }catch(IOException ex){System.out.println("Chat application error: "+safeMessage(ex));}
        catch(IllegalArgumentException ex){System.out.println("Invalid input: "+ex.getMessage());}
    }
    private static void runServer(String[] args,InputHandler input,BufferedReader console)throws IOException{
        int port=args.length>1?parsePort(args[1]):parsePort(input.readLine("Port (0 for an available port): "));
        ChatServer server=new ChatServer(port);
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable(){@Override public void run(){server.close();}},"chat-server-shutdown"));
        int boundPort=server.start();System.out.println("Chat server listening on port "+boundPort+". Press Enter to stop.");
        try{console.readLine();}finally{server.close();}
    }
    private static void runClient(String[] args,InputHandler input,BufferedReader console)throws IOException{
        String host=args.length>1?args[1]:input.readLine("Server host or IP: ");
        int port=args.length>2?parsePort(args[2]):parsePort(input.readLine("Server port: "));
        String username=args.length>3?args[3]:input.readLine("Username (3-16 letters, digits, or underscore): ");
        try(ChatClient client=new ChatClient(host,port,username)){
            System.out.println(client.getGreeting().toDisplayString());
            System.out.println("Connected. Type /help for commands and /quit to leave.");
            client.runInteractive(console,System.out);
        }catch(IOException ex){System.out.println("Could not connect to chat: "+safeMessage(ex));}
    }
    private static int parsePort(String value){
        try{int port=Integer.parseInt(value);if(port<0||port>65535)throw new NumberFormatException();return port;}
        catch(NumberFormatException ex){throw new IllegalArgumentException("Port must be a number from 0 to 65535.");}
    }
    private static String safeMessage(IOException ex){return ex.getMessage()==null?"check the server address and connection":ex.getMessage();}
}
