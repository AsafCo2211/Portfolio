# Asaf Cohen – Portfolio

Hi 👋 I'm Asaf, a Software Engineering student at Ben-Gurion University.  
This repository contains selected projects from my coursework and self-learning.

## 📂 Projects
- **Data Structures (Java):** Implementations of Stack, Queue, Linked List, and Binary Search Tree.  
- **Object-Oriented Programming (Java):** Small console-based project demonstrating inheritance, polymorphism, and encapsulation.  
- **Data Engineering (Python):** Simple ETL pipeline for CSV data cleaning and analysis.  
- **Combinatorics & Logic (Python):** Solutions to algorithmic and mathematical problems.  
- **[World Cup 2026 – Distributed STOMP System](#world-cup-2026--distributed-stomp-system):** Full-stack real-time sports event broadcaster — C++ client, Java concurrent server (TPC + Reactor/NIO), Python/SQLite persistence layer.

## 🛠️ Skills
- Languages: Python, Java, C++  
- Concepts: Data Structures, Algorithms, OOP, Databases, Concurrency, Networking, Software Engineering Principles  
- Tools: Git, GitHub, Maven, Boost, SQLite

---

💡 I'm currently seeking a student position in software development.  
Feel free to connect with me on [LinkedIn](https://www.linkedin.com/in/asaf-cohen-175aa024b/).

---

# World Cup 2026 – Distributed STOMP System

A full-stack, real-time sports event broadcasting system built around the
**STOMP 1.2** (Simple Text Oriented Messaging Protocol) standard.
Multiple clients subscribe to game channels, report live events, and generate
match summaries — all routed through a concurrent Java message broker backed by
a persistent Python/SQLite database.

---

## Architecture

The system is a **3-tier distributed application**:

```
┌─────────────────────────┐        STOMP/TCP         ┌─────────────────────────────┐
│   C++ Client            │ ◄──────────────────────► │   Java STOMP Server         │
│   (StompWCIClient)      │       port 7777           │   (TPC or Reactor mode)     │
└─────────────────────────┘                           └────────────┬────────────────┘
                                                                   │ raw SQL / TCP
                                                                   │ port 7778
                                                      ┌────────────▼────────────────┐
                                                      │   Python SQL Bridge         │
                                                      │   (SQLite via socket)       │
                                                      └─────────────────────────────┘
```

### Tier 1 — C++ Client (`client/`)

A multi-threaded terminal client that speaks STOMP 1.2 over TCP using
**Boost.Asio** for non-blocking I/O.

- **Two threads**: a keyboard thread that reads user commands and a listener
  thread that reads server frames concurrently.
- **RAII & Rule of Five**: `StompProtocol` is explicitly marked non-copyable
  and non-movable (`= delete`) to prevent accidental shared ownership of mutex
  and connection state.
- **Thread Safety**: shared flags use `std::atomic<bool>` (not `volatile`);
  all protocol state is protected by `std::mutex` with `std::lock_guard`.
- **Reconnect lifecycle**: after logout or an ERROR frame, the listener signals
  via an atomic flag and the main loop joins/cleans up before allowing a new
  `login` — no process restart needed.
- **Sorted event history**: incoming MESSAGE frames are stored in a
  `std::vector<Event>` kept sorted by event time using `std::lower_bound`,
  with a halftime tiebreaker for same-time events.

### Tier 2 — Java STOMP Server (`server/`)

A concurrent message broker supporting two interchangeable concurrency models:

| Mode | Class | Description |
|------|-------|-------------|
| `tpc` | `BlockingConnectionHandler` | One dedicated thread per client (simple, blocking I/O) |
| `reactor` | `NonBlockingConnectionHandler` + `Reactor` | NIO Selector thread + `ActorThreadPool` worker pool |

**Reactor Pattern (NIO)**: The `Reactor` registers `SocketChannel`s with a
`Selector`. On readable events it calls `continueRead()` which returns a
`Runnable` dispatched to the `ActorThreadPool`. On writable events it calls
`continueWrite()` which drains the per-connection write queue. When
`shouldTerminate()` is true and the queue is empty, `connections.disconnect()`
is called — ensuring the ERROR frame is fully delivered before the socket is
closed.

**Per-subscriber MESSAGE frames**: `ConnectionsImpl.send(channel, msg)` builds
a personalised copy of each MESSAGE frame for every subscriber, inserting their
unique `subscription:<id>` header right after the command line — no fragile
string replacement.

**Thread-safe connection lifecycle**: `ConnectionsImpl` is a thread-safe
singleton (`ConcurrentHashMap` for both active connections and channel
subscriptions). Cleanup (`disconnect()`) is idempotent and removes the client
from all maps.

### Tier 3 — Python SQL Bridge (`data/sql_server.py`)

A lightweight TCP server that receives null-terminated SQL strings, executes
them against a local **SQLite** database (`stomp_server.db`), and returns
results in a `SUCCESS|row1|row2` format.

Tables:

| Table | Purpose |
|-------|---------|
| `users` | Registered usernames, hashed passwords, registration date |
| `login_history` | Per-session login/logout timestamps (primary key: `id AUTOINCREMENT`) |
| `file_tracking` | Tracks which JSON report files were uploaded per channel |

---

## Prerequisites

| Tool | Version | Notes |
|------|---------|-------|
| Python | 3.8+ | Standard library only (`sqlite3`, `socket`, `threading`) |
| Java JDK | 11+ | Maven wrapper included |
| Maven | 3.6+ | Used to compile and run the server |
| g++ | 11+ | C++11 standard required |
| Boost | 1.69+ | `boost::asio` for TCP I/O |

Install Boost on macOS:
```bash
brew install boost
```

Install Boost on Ubuntu/Debian:
```bash
sudo apt-get install libboost-all-dev
```

---

## How to Run

### Step 1 — Start the Python SQL Bridge

```bash
cd data/
python3 sql_server.py
# Listening on 127.0.0.1:7778
```

The server must be running before the Java server starts. It initialises the
SQLite schema automatically on first run.

### Step 2 — Start the Java STOMP Server

Open a new terminal:

```bash
cd server/
mvn compile

# Thread-Per-Client mode (simpler, good for testing):
mvn exec:java -Dexec.mainClass="bgu.spl.net.impl.stomp.StompServer" \
              -Dexec.args="7777 tpc"

# Reactor / NIO mode (production-grade, non-blocking):
mvn exec:java -Dexec.mainClass="bgu.spl.net.impl.stomp.StompServer" \
              -Dexec.args="7777 reactor"
```

The server prints `Server started` when it is ready to accept connections on
port `7777`.

### Step 3 — Build and Run the C++ Client

Open a new terminal for each client:

```bash
cd client/
make
./bin/StompWCIClient
```

---

## Command Guide

All commands are typed interactively at the client prompt.

### `login`

Connect to the server and authenticate (or auto-register a new user).

```
login <host:port> <username> <password>
```

```
login 127.0.0.1:7777 alice secret123
```

Expected output: `Login successful`

### `join`

Subscribe to a game channel to receive live event broadcasts.

```
join <GameChannelName>
```

```
join Germany_Japan
```

Expected output: `Joined channel Germany_Japan`

The channel name is the exact string `<TeamA>_<TeamB>` as it appears in the
JSON event file (case-sensitive).

### `report`

Parse a JSON event file and broadcast all events to the subscribed channel.
You must already be joined to that channel.

```
report <path/to/events.json>
```

```
report data/events1.json
```

### `summary`

Generate a match summary for a game channel, filtered to events reported by a
specific user, and write it to a file.

```
summary <GameChannelName> <username> <output_file>
```

```
summary Germany_Japan alice /tmp/germany_japan_summary.txt
```

The summary includes:
- Final game statistics (from the last event's `general game updates`)
- Final team statistics for both teams
- Chronological list of all events with descriptions

### `logout`

Gracefully disconnect from the server (sends a STOMP DISCONNECT frame and waits
for the RECEIPT before closing).

```
logout
```

Expected output: `Logout successful.`

After logout, you can issue a new `login` command without restarting the client.

### `exit` (unsubscribe)

Unsubscribe from a specific channel while staying connected.

```
exit <GameChannelName>
```

```
exit Germany_Japan
```

---

## Project Structure

```
SPL-Assignment3/
├── client/
│   ├── src/
│   │   ├── StompClient.cpp        # main(), keyboard thread, listener thread
│   │   ├── StompProtocol.cpp      # STOMP frame builder/parser, event history
│   │   ├── ConnectionHandler.cpp  # Boost.Asio TCP socket wrapper
│   │   └── event.cpp              # Event model + JSON parser
│   ├── include/
│   │   ├── StompProtocol.h
│   │   ├── ConnectionHandler.h
│   │   ├── event.h
│   │   └── json.hpp               # nlohmann/json (header-only)
│   ├── data/
│   │   └── events1.json           # Sample Germany vs Japan match data
│   └── makefile
│
├── server/
│   ├── src/main/java/bgu/spl/net/
│   │   ├── impl/stomp/
│   │   │   ├── StompServer.java               # Entry point, TPC/Reactor selector
│   │   │   ├── StompMessagingProtocolImpl.java # Per-connection STOMP logic
│   │   │   └── StompEncoderDecoder.java        # Frame serialisation
│   │   ├── impl/data/
│   │   │   ├── Database.java       # Singleton; bridges to Python SQL server
│   │   │   ├── User.java
│   │   │   └── LoginStatus.java
│   │   └── srv/
│   │       ├── ConnectionsImpl.java            # Thread-safe connection registry
│   │       ├── BlockingConnectionHandler.java  # TPC handler
│   │       ├── NonBlockingConnectionHandler.java # Reactor handler (NIO)
│   │       ├── Reactor.java                    # NIO Selector loop
│   │       └── ActorThreadPool.java            # Per-actor task serialisation
│   └── pom.xml
│
└── data/
    └── sql_server.py   # Python TCP→SQLite bridge
```

---

## Design Decisions

| Decision | Rationale |
|----------|-----------|
| `std::atomic<bool>` for `isRunning` | `volatile` does not provide memory ordering guarantees in C++ — `atomic` does |
| Two-phase username commit (`pendingUser_`) | `currentUser` is only set after the server sends `CONNECTED`, matching the STOMP spec |
| Events stored only on MESSAGE receipt | Prevents duplicates: the reporter's own broadcast comes back as a server MESSAGE |
| Per-subscriber frame construction | Safe against message bodies containing the text `subscription:null` |
| `connections.disconnect()` at handler exit | Both `BlockingConnectionHandler.run()` finally block and `NonBlockingConnectionHandler.continueWrite()` call `disconnect()`, making cleanup idempotent and leak-free |
| SQLite `UPDATE` via rowid subquery | Standard SQLite does not support `UPDATE ... ORDER BY ... LIMIT` |
