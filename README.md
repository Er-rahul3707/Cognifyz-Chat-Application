# Java Client-Server Chat Application

## Project overview

A multi-client TCP chat application implemented in Java for Project 04 of the Cognifyz Java Development Internship. It supports public chat, private messages, user presence, and basic chat commands.

## Internship task description

This project demonstrates Java socket programming, concurrent client handling, thread-safe session management, and a line-oriented UTF-8 chat protocol.

## Features

- TCP server using `ServerSocket` and clients using `Socket`
- Multiple concurrent clients with one dedicated worker thread per connection
- Unique case-insensitive usernames (3-16 letters, digits, or underscore)
- Public broadcast, join/leave announcements, and private messages
- `/users`, `/msg username message`, `/help`, and `/quit` commands
- Graceful server shutdown and cleanup after unexpected disconnects
- UTF-8 line protocol and server logs that record connection events without chat message contents

## Technology stack

Java 8, Maven, TCP sockets, `ServerSocket`, multithreading, UTF-8, JUnit 4.

## Client-server architecture

- `Main`: starts server or client modes
- `ChatServer`: accepts sockets and manages shutdown
- `ChatClient`: connects, sends messages, and runs the receiver loop
- `ClientHandler`: owns one server-side client socket and its worker thread
- `MessageHandler`: processes public chat lines and commands
- `UserManager`: thread-safe registry, broadcasts, and private routing
- `ChatMessage`: line-safe message representation and wire formatting
- `InputHandler`: console line input
- `ChatSession`: session contract used by the registry and routing logic

## Networking workflow

The client sends its username as the first UTF-8 line. The server validates and registers it, then replies with an `INFO` or `ERROR` message. Subsequent lines are treated as commands or public messages. The server emits protocol frames with `TYPE|sender|text`; the third field may contain `|` characters. TCP delivers each line reliably in order. The application does not encrypt network traffic; use it only on trusted networks unless a secure transport layer is added.

## Project structure

```text
Level-3/Task-1-ChatApplication/
├── .gitignore
├── LICENSE
├── README.md
├── pom.xml
└── src/
    ├── main/java/com/rahul/cognifyz/chat/
    │   ├── ChatClient.java
    │   ├── ChatMessage.java
    │   ├── ChatServer.java
    │   ├── ChatSession.java
    │   ├── ClientHandler.java
    │   ├── InputHandler.java
    │   ├── Main.java
    │   ├── MessageHandler.java
    │   └── UserManager.java
    └── test/java/com/rahul/cognifyz/chat/
        ├── ChatApplicationTest.java
        └── ChatIntegrationTest.java
```

## Installation

Install a Java 8 or newer JDK and Maven 3.6 or newer. The Maven compiler source and target are set to Java 8.

## Build and test

From this project directory:

```sh
mvn clean package
mvn test
```

## Start the server

```sh
java -jar target/chat-application-1.0.0.jar server 5000
```

The server binds to the local machine's available interfaces. Use port `0` to request an ephemeral port; the chosen port is printed at startup. Press Enter in the server terminal to stop it.

## Connect clients

In separate terminals, connect clients to the server's reachable IP address and port:

```sh
java -jar target/chat-application-1.0.0.jar client 127.0.0.1 5000 Alice
java -jar target/chat-application-1.0.0.jar client 127.0.0.1 5000 Bob
```

You can also run the jar without arguments to choose a role and enter connection details interactively. Configure firewalls before allowing connections from other machines.

## Available chat commands

- `/msg username message` sends a private message
- `/users` lists connected usernames
- `/help` displays command help
- `/quit` disconnects gracefully
- Any other line is broadcast to all connected clients

## Sample chat session

```text
[INFO] SERVER: Welcome, Alice. Use /help to see commands.
[INFO] SERVER: Bob joined the chat.
[MSG] Alice: Hello everyone!
[PRIVATE] Bob: Hi Alice, this is private.
```

## Testing

Run `mvn clean test`. The JUnit suite covers username rules, duplicate prevention, user registration and removal, message formatting, public and private delivery, unknown recipients, disconnects, multiple simultaneous connections, and a two-client loopback integration test on an ephemeral server port.

## Limitations

Chat content travels as unencrypted TCP text. Usernames are in-memory only and are not authenticated; another person can claim a username after it becomes available. There is no message history, persistence, rate limiting, or moderation. A production deployment should add TLS, authentication, abuse controls, and operational monitoring. Server logs record connection events and usernames, not chat message bodies.

## Author

Rahul Chandra Sharma
