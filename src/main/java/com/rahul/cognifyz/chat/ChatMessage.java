package com.rahul.cognifyz.chat;

/** A line-safe message exchanged by chat clients and the server. */
public final class ChatMessage {
    public enum Type { INFO, MSG, PRIVATE, ERROR }
    private final Type type;
    private final String sender;
    private final String text;

    public ChatMessage(Type type, String sender, String text) {
        if (type == null || sender == null || text == null) throw new IllegalArgumentException("Message fields are required.");
        if (containsLineBreak(sender) || containsLineBreak(text)) throw new IllegalArgumentException("Messages must fit on one line.");
        this.type=type; this.sender=sender; this.text=text;
    }
    public static ChatMessage info(String text) { return new ChatMessage(Type.INFO,"SERVER",text); }
    public static ChatMessage error(String text) { return new ChatMessage(Type.ERROR,"SERVER",text); }
    public static ChatMessage publicMessage(String sender,String text) { return new ChatMessage(Type.MSG,sender,text); }
    public static ChatMessage privateMessage(String sender,String text) { return new ChatMessage(Type.PRIVATE,sender,text); }
    public Type getType(){return type;} public String getSender(){return sender;} public String getText(){return text;}
    public String format(){return type.name()+"|"+sender+"|"+text;}
    public String toDisplayString(){return "["+type.name()+"] "+sender+": "+text;}
    public static ChatMessage parse(String line) {
        if(line==null) throw new IllegalArgumentException("Message line is required.");
        String[] fields=line.split("\\|",3);
        if(fields.length!=3) throw new IllegalArgumentException("Invalid chat message format.");
        return new ChatMessage(Type.valueOf(fields[0]),fields[1],fields[2]);
    }
    private static boolean containsLineBreak(String value){return value.indexOf('\n')>=0||value.indexOf('\r')>=0;}
    @Override public String toString(){return toDisplayString();}
}
