#include <stdlib.h>
#include "../include/ConnectionHandler.h"
#include "../include/StompProtocol.h"
#include <atomic>   // C1: std::atomic instead of volatile
#include <thread>
#include <iostream>
#include <string>
#include <sstream>

using namespace std;

// C1: std::atomic<bool> provides correct memory ordering across threads.
//     'volatile' does NOT provide thread safety in C++.
static atomic<bool> isRunning{true};

// C4: separate flag that the listener sets when a connection ends (logout or
//     error).  The main loop checks this and cleans up so the user can log in
//     again without restarting the client.
static atomic<bool> connectionEnded{false};

/**
 * @brief Listener thread: reads STOMP frames from the server and dispatches
 *        them to the protocol for processing.
 *
 * C4: This thread no longer sets isRunning=false.  Instead it sets
 *     connectionEnded=true so that the main loop can clean up and allow a
 *     fresh login without restarting the client.
 */
void connectionReader(ConnectionHandler* handler, StompProtocol* protocol) {
    while (isRunning) {
        string answer;
        char ch;
        bool readOk = true;

        // Read one complete STOMP frame (terminated by the null byte '\0')
        while (true) {
            if (!handler->getBytes(&ch, 1)) {
                // Socket closed or error
                readOk = false;
                break;
            }
            if (ch == '\0') break;
            answer += ch;
        }

        if (!readOk) {
            cout << "Disconnected from server." << endl;
            break;
        }
        if (!isRunning) break;

        // C2: processServerResponse internally acquires the protocol mutex.
        string output = protocol->processServerResponse(answer);
        if (!output.empty()) {
            cout << output << endl;
        }

        // C4: after processing, check whether the protocol's connection is
        //     still active.  isConnected becomes false on CONNECTED receipt for
        //     DISCONNECT or on any ERROR frame.
        if (!protocol->isUserLoggedIn()) {
            handler->close();
            break;
        }
    }

    // Signal the main loop that this connection is done.
    connectionEnded = true;
}

int main(int argc, char *argv[]) {
    StompProtocol protocol;
    ConnectionHandler* connectionHandler = nullptr;
    thread*            listenerThread    = nullptr;

    while (isRunning) {
        // C4 + C10: if the listener thread finished (logout receipt or error),
        //           join it, delete the old handler, and reset pointers so the
        //           user can issue a new 'login' command.
        if (connectionEnded.load() && listenerThread) {
            listenerThread->join();
            delete listenerThread;  listenerThread    = nullptr;
            delete connectionHandler; connectionHandler = nullptr;
            connectionEnded = false;
            // protocol state has already been cleaned up inside processServerResponse
        }

        const short bufsize = 1024;
        char buf[bufsize];
        cin.getline(buf, bufsize);

        // Handle EOF (Ctrl+D) — only then do we exit the program
        if (cin.eof()) {
            isRunning = false;
            break;
        }

        string line(buf);
        if (line.empty()) continue;

        stringstream ss(line);
        string command;
        ss >> command;

        // ---------------------------------------------------------------- login
        if (command == "login") {
            // C4 + C10: if a previous listener is still running (edge case:
            //           user types 'login' before listener has set connectionEnded),
            //           reject until cleanup has occurred.
            if (connectionHandler != nullptr) {
                cout << "The client is already logged in, log out before trying again" << endl;
                continue;
            }

            // Parse host:port (rest of line is forwarded to protocol for frame building)
            string hostPort;
            ss >> hostPort;
            if (hostPort.empty()) {
                cout << "Usage: login {host:port} {username} {password}" << endl;
                continue;
            }

            string host;
            short  port = 0;
            size_t colonPos = hostPort.find(':');
            if (colonPos == string::npos) {
                cout << "Invalid host:port format" << endl;
                continue;
            }
            host = hostPort.substr(0, colonPos);
            try {
                port = static_cast<short>(stoi(hostPort.substr(colonPos + 1)));
            } catch (...) {
                cout << "Invalid port number" << endl;
                continue;
            }

            connectionHandler = new ConnectionHandler(host, port);
            if (!connectionHandler->connect()) {
                cout << "Could not connect to server" << endl;
                delete connectionHandler;
                connectionHandler = nullptr;
                continue;
            }

            // Build and send the CONNECT frame.
            // C2: processUserInput acquires the protocol mutex internally.
            string frame = protocol.processUserInput(line);
            if (frame.empty()) {
                // protocol rejected the command (e.g. already logged in at protocol level)
                delete connectionHandler;
                connectionHandler = nullptr;
                continue;
            }

            if (!connectionHandler->sendBytes(frame.c_str(), static_cast<int>(frame.length()))) {
                cout << "Could not connect to server" << endl;
                delete connectionHandler;
                connectionHandler = nullptr;
                continue;
            }

            // C4: start a fresh listener thread for this connection
            connectionEnded  = false;
            listenerThread   = new thread(connectionReader, connectionHandler, &protocol);

        // --------------------------------------------------------- other commands
        } else {
            // C2: processUserInput acquires the protocol mutex internally.
            string frames = protocol.processUserInput(line);

            if (!frames.empty()) {
                if (connectionHandler != nullptr) {
                    // A 'report' produces multiple null-terminated frames concatenated.
                    // Split on '\0' and send each frame individually.
                    size_t start = 0;
                    size_t end   = frames.find('\0');
                    while (end != string::npos) {
                        size_t len = end - start + 1;  // include the '\0'
                        if (!connectionHandler->sendBytes(&frames[start], static_cast<int>(len))) {
                            cout << "Error sending data." << endl;
                            break;
                        }
                        start = end + 1;
                        end   = frames.find('\0', start);
                    }
                } else {
                    cout << "Please login first." << endl;
                }
            }
        }
    }

    // Final cleanup — wait for the listener thread if it is still running
    if (listenerThread) {
        listenerThread->join();
        delete listenerThread;
    }
    if (connectionHandler) {
        delete connectionHandler;
    }

    return 0;
}
