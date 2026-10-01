# Length-Prefixed TCP Messenger

A small Java networking project that demonstrates how to build message framing on top of TCP.

The server accepts exactly two clients and forwards messages between them. Each client can send and receive messages simultaneously using blocking sockets and basic Java threads.

## What This Project Demonstrates

TCP provides a stream of bytes rather than discrete messages.

A single `write()` on one side does not guarantee that the receiving side will get the same data in a single `read()` call.

To recover message boundaries, this project uses a simple length-prefixed frame format:

```text
[4-byte payload length][UTF-8 payload]
```

The 4-byte length is stored in big-endian order and represents the number of UTF-8 encoded bytes in the payload.

For example:

```text
00 00 00 05 48 65 6C 6C 6F
```

represents:

```text
length = 5
payload = "Hello"
```

## Architecture

```text
Client 1
   │
   │ framed TCP messages
   ▼
┌──────────────┐
│    Server    │
│              │
│ Client 1 ───────► Client 2
│ Client 2 ───────► Client 1
└──────────────┘
   ▲
   │
   │ framed TCP messages
   │
Client 2
```

The server uses two execution paths:

```text
Thread:      Client 1 → Client 2
Main thread: Client 2 → Client 1
```

Each client also uses two execution paths:

```text
Main thread:     keyboard input → socket
Receiver thread: socket → terminal output
```

This allows both clients to send and receive messages independently while still using ordinary blocking sockets.

## Framing

Sending a message consists of:

1. Encoding the string as UTF-8.
2. Calculating the payload length in bytes.
3. Writing the length as four big-endian bytes.
4. Writing the payload bytes.

On the receiving side:

```text
read exactly 4 bytes
        ↓
decode payload length
        ↓
validate length
        ↓
read exactly N bytes
        ↓
decode UTF-8 payload
```

## Handling Partial Reads

A key part of the project is `readExactly()`.

Calling:

```java
in.read(buffer);
```

does not guarantee that the entire buffer will be filled.

For example, a four-byte frame header could arrive as:

```text
1 byte
2 bytes
1 byte
```

The receiver therefore keeps reading until the required number of bytes has been collected.

If EOF is reached before the full frame arrives, the connection is treated as incomplete rather than looping forever.

## Message Length Limit

Before allocating memory for a payload, the decoded frame length is validated.

The current maximum frame size is:

```text
1 MiB
```

This prevents malformed input from requesting an unreasonable allocation.

## Running the Project

### Requirements

- Java JDK
- No external dependencies

### 1. Start the server

Run:

```text
TcpServer
```

The server waits for exactly two clients:

```text
Waiting for client 1...
Client 1 connected
Waiting for client 2...
Client 2 connected
Both clients connected
```

### 2. Start two clients

Run two instances of:

```text
TcpClient
```

Each client connects to:

```text
localhost:5000
```

### 3. Send messages

Type a message into either client console and press Enter.

Example:

```text
hello from client 1
```

The other client receives:

```text
Received: hello from client 1
```

Messages can be sent in either direction.

UTF-8 text is also supported:

```text
hello 👋
```

## Verification

The implementation was tested with:

- Messages sent in both directions
- Multiple frames sent back-to-back
- UTF-8 / non-ASCII messages
- Artificially limited reads to force frames to arrive in multiple pieces

The short-read experiment confirmed that the framing logic still works even when a single frame requires multiple `read()` calls.

## Scope

This is intentionally a small networking exercise.

It does not include:

- Usernames
- Chat rooms
- Persistent message history
- Reconnection logic
- Encryption
- GUI
- Arbitrary numbers of clients
- NIO
- Networking frameworks

The goal is specifically to understand TCP byte streams, message framing, blocking I/O, and basic bidirectional communication.

## Key Takeaway

> TCP gives you bytes, not messages.

Application-level protocols must define their own message boundaries.

In this project, a four-byte length prefix provides those boundaries and allows the receiver to reconstruct messages reliably regardless of how TCP divides the underlying byte stream.