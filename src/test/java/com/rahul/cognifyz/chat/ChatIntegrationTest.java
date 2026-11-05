package com.rahul.cognifyz.chat;

import java.io.IOException;
import org.junit.Test;
import static org.junit.Assert.*;

/** End-to-end loopback check with two concurrently connected TCP clients. */
public class ChatIntegrationTest {
    @Test public void twoClientsConnectAndExchangePublicAndPrivateMessages()throws Exception{
        ChatServer server=new ChatServer(0);ChatClient alice=null,bob=null;
        try{
            int port=server.start();alice=new ChatClient("127.0.0.1",port,"AliceTest");alice.setReadTimeout(3000);
            assertTrue(alice.getGreeting().getText().contains("Welcome"));assertMessage(alice,ChatMessage.Type.INFO,"AliceTest joined");
            bob=new ChatClient("127.0.0.1",port,"BobTest");bob.setReadTimeout(3000);
            assertTrue(bob.getGreeting().getText().contains("Welcome"));assertMessage(bob,ChatMessage.Type.INFO,"BobTest joined");
            assertMessage(alice,ChatMessage.Type.INFO,"BobTest joined");
            alice.sendMessage("hello Bob");assertMessage(alice,ChatMessage.Type.MSG,"hello Bob");assertMessage(bob,ChatMessage.Type.MSG,"hello Bob");
            alice.sendMessage("/msg BobTest private hello");assertMessage(bob,ChatMessage.Type.PRIVATE,"private hello");assertMessage(alice,ChatMessage.Type.INFO,"Private message sent");
            bob.sendMessage("/quit");assertMessage(bob,ChatMessage.Type.INFO,"Goodbye");assertMessage(alice,ChatMessage.Type.INFO,"BobTest left");
        }finally{if(alice!=null)alice.close();if(bob!=null)bob.close();server.close();}
    }
    private static ChatMessage assertMessage(ChatClient client,ChatMessage.Type type,String text)throws IOException{ChatMessage message=client.readMessage();assertNotNull("Expected "+type+" message containing "+text,message);assertEquals(message.toString(),type,message.getType());assertTrue(message.toString(),message.getText().contains(text));return message;}
}
