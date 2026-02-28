#!/usr/bin/env python3
"""
Basic Python Server for STOMP Assignment – Stage 3.3

This server acts as a database backend for the Java STOMP server.
It listens on a TCP port, receives raw SQL commands as null-terminated strings,
executes them against a local SQLite database, and returns the results.

IMPORTANT:
DO NOT CHANGE the server name or the basic protocol.
Students should EXTEND this server by implementing the methods below.
"""

import socket
import sys
import threading
import sqlite3
import os

# Server constants
SERVER_NAME = "STOMP_PYTHON_SQL_SERVER"  # DO NOT CHANGE!
DB_FILE = "stomp_server.db"              # DO NOT CHANGE!


def recv_null_terminated(sock: socket.socket) -> str:
    """
    Receives data from a socket until a null byte ('\0') is encountered.

    This function buffers incoming data in chunks. Once the delimiter is found,
    it splits the buffer, returns the decoded string (up to the delimiter),
    and discards the rest (simplification for this specific protocol).

    Args:
        sock (socket.socket): The connected client socket.

    Returns:
        str: The decoded message string without the null terminator.
             Returns an empty string if the connection is closed.
    """
    data = b""
    while True:
        chunk = sock.recv(1024)
        if not chunk:
            return ""
        data += chunk
        if b"\0" in data:
            msg, _ = data.split(b"\0", 1)
            return msg.decode("utf-8", errors="replace")


def init_database():
    """
    Initialize the SQLite database schema if it doesn't exist.
    
    Creates the following tables:
    1. users: Stores username, password, and registration date.
    2. login_history: Tracks login/logout timestamps for users.
    3. file_tracking: Tracks files uploaded by users to specific game channels.
    """
    conn = sqlite3.connect(DB_FILE)
    c = conn.cursor()

    # Create 'users' table
    c.execute('''CREATE TABLE IF NOT EXISTS users (
                    username TEXT PRIMARY KEY,
                    password TEXT NOT NULL,
                    registration_date DATETIME 
                )''')

    # Create 'login_history' table
    # L3: Add an explicit primary key so that the Java server's UPDATE subquery
    #     (UPDATE ... WHERE rowid = (SELECT rowid FROM login_history ...)) can
    #     reliably target exactly one row via the implicit rowid alias.
    c.execute('''CREATE TABLE IF NOT EXISTS login_history (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT,
                    login_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    logout_time DATETIME
                )''')

    # Create 'file_tracking' table
    c.execute('''CREATE TABLE IF NOT EXISTS file_tracking (
                    username TEXT NOT NULL,
                    filename TEXT NOT NULL,
                    upload_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    game_channel TEXT NOT NULL,
                    PRIMARY KEY (username, filename)
                )''')
    
    conn.commit()
    conn.close()
    print(f"[{SERVER_NAME}] Database initialized.")


def execute_sql_command(sql_command: str) -> str:
    """
    Executes a raw SQL command received from the client.

    WARNING: This function executes raw SQL strings directly. 
    It is vulnerable to SQL Injection by design (for this assignment).

    Args:
        sql_command (str): The raw SQL query to execute.

    Returns:
        str: A response string formatted for the Java client:
             - "SUCCESS": For INSERT/UPDATE/DELETE commands.
             - "SUCCESS|row1_data|row2_data": For SELECT queries.
             - "ERROR: ...": If an exception occurs.
    """
    conn = sqlite3.connect(DB_FILE)
    c = conn.cursor()

    try:
        # Log the command being executed
        print(f"[{SERVER_NAME}] Executing SQL: {sql_command}")
        
        c.execute(sql_command)
        
        # Handle SELECT queries (returning data)
        if sql_command.strip().upper().startswith("SELECT"):
            rows = c.fetchall()
            if not rows:
                return "SUCCESS"
            
            # Format data for Java: Rows separated by '|', Columns separated by ', '
            # Example: "SUCCESS|user1, pass1|user2, pass2"
            data_str = "|".join([", ".join(map(str, row)) for row in rows])
            return "SUCCESS|" + data_str
        else:
            # Handle modification queries (INSERT, UPDATE, DELETE)
            conn.commit()
            return "SUCCESS"
            
    except Exception as e:
        print(f"[{SERVER_NAME}] SQL Error: {e}")
        return f"ERROR: {e}"
    finally:
        conn.close()


def execute_sql_query(sql_query: str) -> str:
    """
    Placeholder for specific query handling if needed in the future.
    Currently unused as execute_sql_command handles everything.
    """
    return "done"


def handle_client(client_socket: socket.socket, addr):
    """
    Main loop for handling a single client connection.

    Reads a null-terminated SQL string, executes it, and sends back
    the null-terminated response.

    Args:
        client_socket (socket.socket): The client's socket object.
        addr (tuple): The client's address (IP, Port).
    """
    print(f"[{SERVER_NAME}] Client connected from {addr}")

    try:
        while True:
            message = recv_null_terminated(client_socket)
            if message == "":
                break

            response = execute_sql_command(message)
            client_socket.sendall(response.encode("utf-8") + b"\0")

    except Exception as e:
        print(f"[{SERVER_NAME}] Error handling client {addr}: {e}")
    finally:
        try:
            client_socket.close()
        except Exception:
            pass
        print(f"[{SERVER_NAME}] Client {addr} disconnected")


def start_server(host="127.0.0.1", port=7778):
    """
    Starts the multi-threaded TCP server.

    Args:
        host (str): IP address to bind to (default: localhost).
        port (int): Port number to listen on (default: 7778).
    """
    init_database()
    server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    # Allow immediate reuse of the port after stopping the server
    server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)

    try:
        server_socket.bind((host, port))
        server_socket.listen(5)
        print(f"[{SERVER_NAME}] Server started on {host}:{port}")
        print(f"[{SERVER_NAME}] Waiting for connections...")

        while True:
            client_socket, addr = server_socket.accept()
            # Handle each client in a separate thread
            t = threading.Thread(
                target=handle_client,
                args=(client_socket, addr),
                daemon=True
            )
            t.start()

    except KeyboardInterrupt:
        print(f"\n[{SERVER_NAME}] Shutting down server...")
    finally:
        try:
            server_socket.close()
        except Exception:
            pass


if __name__ == "__main__":
    port = 7778
    # Allow port configuration via command line arguments
    if len(sys.argv) > 1:
        raw_port = sys.argv[1].strip()
        try:
            port = int(raw_port)
        except ValueError:
            print(f"Invalid port '{raw_port}', falling back to default {port}")

    start_server(port=port)