#pragma once

#include "../include/ConnectionHandler.h"
#include "../include/event.h"
#include <string>
#include <vector>
#include <map>
#include <mutex>    // C2: mutex for thread safety between keyboard and listener threads
#include <algorithm> // C6: std::lower_bound for sorted event insertion

/**
 * @class StompProtocol
 * @brief Manages the STOMP protocol logic for the client.
 *
 * Thread-safety: processUserInput() is called from the main (keyboard) thread;
 * processServerResponse() is called from the listener thread.  All shared
 * mutable state is protected by mtx_.
 */
class StompProtocol
{
private:
    bool isConnected;
    int subscriptionIdCounter;
    int receiptIdCounter;

    /**
     * Maps a topic name to its active subscription ID.
     * Key: Topic Name, Value: Subscription ID (int).
     */
    std::map<std::string, int> topicToSubscriptionId;

    /**
     * Maps receipt IDs to a context string ("Joined channel X", "Exited channel X", "DISCONNECT").
     */
    std::map<int, std::string> receiptHandlers;

    /** Username confirmed after CONNECTED is received from the server. */
    std::string currentUser;

    /**
     * Username stored when a login command is typed, confirmed only when
     * CONNECTED arrives.  If login fails the pending name is discarded.
     * C8: currentUser must not be set until CONNECTED is confirmed.
     */
    std::string pendingUser_;

    /**
     * Game-event history, keyed by [gameName][reportingUser].
     * Populated exclusively from incoming MESSAGE frames so that each event
     * is stored exactly once (C3).  Sorted by event time on insertion (C6).
     */
    std::map<std::string, std::map<std::string, std::vector<Event>>> eventsHistory;

    /** C2: Protects all shared state against concurrent thread access. */
    std::mutex mtx_;

    // C15: StompProtocol is non-copyable and non-movable.
    StompProtocol(const StompProtocol&)            = delete;
    StompProtocol& operator=(const StompProtocol&) = delete;
    StompProtocol(StompProtocol&&)                 = delete;
    StompProtocol& operator=(StompProtocol&&)      = delete;

public:
    StompProtocol();

    /**
     * Processes a line of user input from the keyboard.
     * Returns the STOMP frame(s) to send, or "" for local-only actions.
     * Called from the main thread; acquires mtx_.
     */
    std::string processUserInput(std::string userLine);

    /**
     * Processes a complete STOMP frame received from the server.
     * Returns a human-readable string to print, or "" for no output.
     * Called from the listener thread; acquires mtx_.
     */
    std::string processServerResponse(std::string serverFrame);

    /**
     * Returns true when the connection is active (i.e. CONNECTED has been
     * received and no ERROR / DISCONNECT receipt has arrived yet).
     * Used by the listener thread to decide when to exit its loop.
     */
    bool isUserLoggedIn();
    // L1: shouldLogout() removed — it was dead code; callers use isUserLoggedIn() directly.
};
