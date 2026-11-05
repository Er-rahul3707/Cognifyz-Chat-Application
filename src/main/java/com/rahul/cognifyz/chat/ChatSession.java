package com.rahul.cognifyz.chat;

import java.io.IOException;

/** Minimal session contract used by the thread-safe user registry. */
public interface ChatSession {
    String getUsername();
    void send(ChatMessage message) throws IOException;
    void close() throws IOException;
}
