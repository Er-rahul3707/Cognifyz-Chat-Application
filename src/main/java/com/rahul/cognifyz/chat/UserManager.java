package com.rahul.cognifyz.chat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Thread-safe registry and delivery point for connected user sessions. */
public final class UserManager {
    private final ConcurrentMap<String,ChatSession> sessions=new ConcurrentHashMap<String,ChatSession>();

    public static boolean isValidUsername(String username) {
        return username!=null && username.matches("[A-Za-z0-9_]{3,16}");
    }
    public boolean register(String username,ChatSession session) {
        if(!isValidUsername(username)||session==null)return false;
        return sessions.putIfAbsent(key(username),session)==null;
    }
    public boolean remove(String username,ChatSession session) {
        if(username==null||session==null)return false;
        return sessions.remove(key(username),session);
    }
    public ChatSession find(String username) { return username==null?null:sessions.get(key(username)); }
    public List<String> getUsernames() {
        List<String> names=new ArrayList<String>();
        for(ChatSession session:sessions.values())names.add(session.getUsername());
        Collections.sort(names,String.CASE_INSENSITIVE_ORDER);
        return Collections.unmodifiableList(names);
    }
    public int size(){return sessions.size();}
    public void broadcast(ChatMessage message,ChatSession excluded) {
        for(ChatSession session:sessions.values()) {
            if(session==excluded)continue;
            try{session.send(message);}catch(IOException ex){remove(session.getUsername(),session);try{session.close();}catch(IOException ignored){}}
        }
    }
    public boolean sendTo(String username,ChatMessage message) {
        ChatSession session=find(username);if(session==null)return false;
        try{session.send(message);return true;}catch(IOException ex){remove(session.getUsername(),session);try{session.close();}catch(IOException ignored){}return false;}
    }
    public void notifyAndCloseAll(ChatMessage message) {
        for(ChatSession session:sessions.values())try{session.send(message);}catch(IOException ignored){}
        for(ChatSession session:sessions.values())try{session.close();}catch(IOException ignored){}
    }
    private static String key(String username){return username.toLowerCase(Locale.ROOT);}
}
