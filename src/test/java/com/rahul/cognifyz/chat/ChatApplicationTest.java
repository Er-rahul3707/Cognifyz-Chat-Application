package com.rahul.cognifyz.chat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class ChatApplicationTest {
    private UserManager users;
    private MessageHandler messages;

    @Before public void setUp(){users=new UserManager();messages=new MessageHandler(users);}

    @Test public void validatesUsernames(){assertTrue(UserManager.isValidUsername("Rahul_7"));assertFalse(UserManager.isValidUsername("ab"));assertFalse(UserManager.isValidUsername("bad name"));assertFalse(UserManager.isValidUsername("invalid!"));}
    @Test public void preventsDuplicateUsernamesCaseInsensitively(){FakeSession first=new FakeSession("Alex");FakeSession second=new FakeSession("alex");assertTrue(users.register(first.getUsername(),first));assertFalse(users.register(second.getUsername(),second));}
    @Test public void registersUsers(){FakeSession user=new FakeSession("Mira");assertTrue(users.register("Mira",user));assertSame(user,users.find("mira"));assertEquals(1,users.size());}
    @Test public void removesUsers(){FakeSession user=new FakeSession("Mira");users.register("Mira",user);assertTrue(users.remove("MIRA",user));assertNull(users.find("Mira"));assertEquals(0,users.size());}
    @Test public void formatsAndParsesMessages(){ChatMessage original=ChatMessage.publicMessage("Mira","hello | chat");assertEquals("MSG|Mira|hello | chat",original.format());ChatMessage parsed=ChatMessage.parse(original.format());assertEquals(original.getType(),parsed.getType());assertEquals("hello | chat",parsed.getText());}
    @Test public void broadcastsPublicMessages(){FakeSession mira=register("Mira"),noah=register("Noah");messages.handle(mira,"hello everyone");assertEquals("hello everyone",last(noah).getText());assertEquals(ChatMessage.Type.MSG,last(mira).getType());}
    @Test public void routesPrivateMessagesOnlyToRecipient(){FakeSession mira=register("Mira"),noah=register("Noah");messages.handle(mira,"/msg Noah secret hello");assertEquals(ChatMessage.Type.PRIVATE,last(noah).getType());assertEquals("secret hello",last(noah).getText());assertEquals(ChatMessage.Type.INFO,last(mira).getType());}
    @Test public void reportsUnknownPrivateRecipient(){FakeSession mira=register("Mira");messages.handle(mira,"/msg nobody hello");assertEquals(ChatMessage.Type.ERROR,last(mira).getType());assertTrue(last(mira).getText().contains("Unknown recipient"));}
    @Test public void removesClientAfterDisconnect()throws Exception{ChatServer server=new ChatServer(0);try{int port=server.start();ChatClient client=new ChatClient("127.0.0.1",port,"Disconnecting");client.setReadTimeout(3000);client.readMessage();client.sendMessage("/quit");assertEquals("Goodbye.",client.readMessage().getText());waitForUsers(server,0);client.close();}finally{server.close();}}
    @Test public void handlesUnexpectedDisconnectAndNotifiesOtherUsers()throws Exception{
        ChatServer server=new ChatServer(0);ChatClient observer=null,departing=null;
        try{
            int port=server.start();observer=new ChatClient("127.0.0.1",port,"Observer");observer.setReadTimeout(3000);
            assertTrue(observer.getGreeting().getText().contains("Welcome"));assertMessage(observer,ChatMessage.Type.INFO,"Observer joined");
            departing=new ChatClient("127.0.0.1",port,"Departing");departing.setReadTimeout(3000);
            assertTrue(departing.getGreeting().getText().contains("Welcome"));assertMessage(departing,ChatMessage.Type.INFO,"Departing joined");
            assertMessage(observer,ChatMessage.Type.INFO,"Departing joined");departing.close();
            waitForUsers(server,1);assertMessage(observer,ChatMessage.Type.INFO,"Departing left");
        }finally{if(observer!=null)observer.close();if(departing!=null)departing.close();server.close();}
    }
    @Test public void acceptsMultipleSimultaneousClients()throws Exception{
        ChatServer server=new ChatServer(0);ChatClient first=null,second=null;
        try{
            int port=server.start();first=new ChatClient("127.0.0.1",port,"FirstUser");first.setReadTimeout(3000);
            assertTrue(first.getGreeting().getText().contains("Welcome"));assertMessage(first,ChatMessage.Type.INFO,"FirstUser joined");
            second=new ChatClient("127.0.0.1",port,"SecondUser");second.setReadTimeout(3000);
            assertTrue(second.getGreeting().getText().contains("Welcome"));assertMessage(second,ChatMessage.Type.INFO,"SecondUser joined");
            assertMessage(first,ChatMessage.Type.INFO,"SecondUser joined");assertEquals(2,server.getUserManager().size());
            first.sendMessage("multi-client hello");assertMessage(first,ChatMessage.Type.MSG,"multi-client hello");assertMessage(second,ChatMessage.Type.MSG,"multi-client hello");
        }finally{if(first!=null)first.close();if(second!=null)second.close();server.close();}
    }

    private FakeSession register(String name){FakeSession session=new FakeSession(name);assertTrue(users.register(name,session));return session;}
    private static ChatMessage assertMessage(ChatClient client,ChatMessage.Type type,String text)throws IOException{ChatMessage message=client.readMessage();assertNotNull("Expected "+type+" message containing "+text,message);assertEquals(message.toString(),type,message.getType());assertTrue(message.toString(),message.getText().contains(text));return message;}
    private static ChatMessage last(FakeSession session){return session.messages.get(session.messages.size()-1);}
    private static void waitForUsers(ChatServer server,int count)throws InterruptedException{long until=System.currentTimeMillis()+3000;while(server.getUserManager().size()!=count&&System.currentTimeMillis()<until)Thread.sleep(10);assertEquals(count,server.getUserManager().size());}
    private static final class FakeSession implements ChatSession{
        private final String username;private final List<ChatMessage> messages=new ArrayList<ChatMessage>();
        private FakeSession(String username){this.username=username;}
        @Override public String getUsername(){return username;}
        @Override public synchronized void send(ChatMessage message){messages.add(message);}
        @Override public void close(){}
    }
}
