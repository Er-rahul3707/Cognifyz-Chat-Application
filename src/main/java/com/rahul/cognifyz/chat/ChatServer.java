package com.rahul.cognifyz.chat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/** TCP chat server with one worker thread per accepted client. */
public final class ChatServer implements AutoCloseable {
    private static final Logger LOGGER=Logger.getLogger(ChatServer.class.getName());
    private final int requestedPort;
    private final UserManager users=new UserManager();
    private final MessageHandler messages=new MessageHandler(users);
    private final Set<ClientHandler> handlers=Collections.newSetFromMap(new ConcurrentHashMap<ClientHandler,Boolean>());
    private volatile boolean running;
    private ServerSocket serverSocket;
    private Thread acceptThread;

    public ChatServer(int port){if(port<0||port>65535)throw new IllegalArgumentException("Port must be between 0 and 65535.");requestedPort=port;}
    public synchronized int start()throws IOException{
        if(running)throw new IllegalStateException("Server is already running.");
        ServerSocket bound=new ServerSocket();bound.setReuseAddress(true);bound.bind(new InetSocketAddress(requestedPort));
        serverSocket=bound;running=true;
        acceptThread=new Thread(new Runnable(){@Override public void run(){acceptConnections();}},"chat-server-accept");
        acceptThread.setDaemon(true);acceptThread.start();
        LOGGER.info("Chat server listening on port "+serverSocket.getLocalPort());
        return serverSocket.getLocalPort();
    }
    private void acceptConnections(){
        while(running){
            try{
                final Socket socket=serverSocket.accept();
                final ClientHandler handler;
                try{handler=new ClientHandler(socket,users,messages,new java.util.function.Consumer<ClientHandler>(){@Override public void accept(ClientHandler value){handlers.remove(value);}});}
                catch(IOException ex){try{socket.close();}catch(IOException ignored){}throw ex;}
                handlers.add(handler);
                if(!running){handler.close();handlers.remove(handler);continue;}
                Thread worker=new Thread(handler,"chat-client-"+socket.getRemoteSocketAddress());worker.setDaemon(true);worker.start();
            }catch(SocketException ex){if(running)LOGGER.log(Level.WARNING,"Server socket failed.",ex);break;}
            catch(IOException ex){if(running)LOGGER.log(Level.WARNING,"Could not accept client connection.",ex);}
        }
    }
    public boolean isRunning(){return running;}
    public int getPort(){return serverSocket==null?requestedPort:serverSocket.getLocalPort();}
    public UserManager getUserManager(){return users;}
    @Override public synchronized void close(){
        if(!running&&serverSocket==null)return;
        running=false;
        if(serverSocket!=null)try{serverSocket.close();}catch(IOException ignored){}
        if(acceptThread!=null&&Thread.currentThread()!=acceptThread){try{acceptThread.join(2000);}catch(InterruptedException ex){Thread.currentThread().interrupt();}}
        try{users.notifyAndCloseAll(ChatMessage.info("Server is shutting down."));}catch(RuntimeException ex){LOGGER.log(Level.FINE,"Error notifying chat clients.",ex);}
        for(ClientHandler handler:handlers)try{handler.close();}catch(IOException ignored){}
        handlers.clear();
        serverSocket=null;
        LOGGER.info("Chat server stopped.");
    }
}
