package com.rahul.cognifyz.chat;

/** Applies chat commands and routes public or private messages. */
public final class MessageHandler {
    private final UserManager users;
    public MessageHandler(UserManager users){if(users==null)throw new IllegalArgumentException("User manager is required.");this.users=users;}

    /** Returns false when the session requested a graceful quit. */
    public boolean handle(ChatSession sender,String line) {
        if(sender==null||line==null)return false;
        if(line.trim().isEmpty())return true;
        if(line.equals("/quit")){send(sender,ChatMessage.info("Goodbye."));return false;}
        if(line.equals("/users")){send(sender,ChatMessage.info("Connected users: "+join(users.getUsernames())));return true;}
        if(line.equals("/help")){send(sender,ChatMessage.info("Commands: /msg username message, /users, /help, /quit. Any other line is broadcast to everyone."));return true;}
        if(line.startsWith("/msg")){
            String[] parts=line.split("\\s+",3);
            if(parts.length!=3||!"/msg".equals(parts[0])||parts[1].isEmpty()||parts[2].trim().isEmpty()){
                send(sender,ChatMessage.error("Usage: /msg username message"));return true;
            }
            if(!users.sendTo(parts[1],ChatMessage.privateMessage(sender.getUsername(),parts[2])))
                send(sender,ChatMessage.error("Unknown recipient: "+parts[1]));
            else send(sender,ChatMessage.info("Private message sent to "+parts[1]+"."));
            return true;
        }
        if(line.startsWith("/")){send(sender,ChatMessage.error("Unknown command. Use /help for available commands."));return true;}
        users.broadcast(ChatMessage.publicMessage(sender.getUsername(),line),null);
        return true;
    }
    private static void send(ChatSession session,ChatMessage message){try{session.send(message);}catch(java.io.IOException ignored){}}
    private static String join(java.util.List<String> names){return names.isEmpty()?"(none)":joinNames(names);}
    private static String joinNames(java.util.List<String> names){StringBuilder value=new StringBuilder();for(String name:names){if(value.length()>0)value.append(", ");value.append(name);}return value.toString();}
}
