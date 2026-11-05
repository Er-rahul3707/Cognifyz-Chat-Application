package com.rahul.cognifyz.chat;

import java.io.BufferedReader;
import java.io.IOException;

/** Reads console lines without coupling user input to networking classes. */
public final class InputHandler {
    private final BufferedReader reader;
    public InputHandler(BufferedReader reader){if(reader==null)throw new IllegalArgumentException("Reader is required.");this.reader=reader;}
    public String readLine(String prompt)throws IOException{System.out.print(prompt);return reader.readLine();}
}
