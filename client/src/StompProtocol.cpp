#include "../include/StompProtocol.h"
#include "../include/event.h"
#include <sstream>
#include <fstream>
#include <iostream>
#include <vector>
#include <algorithm>  // std::lower_bound (C6)

using namespace std;

// ---------------------------------------------------------------------------
// Helper: time-based comparator for sorted event insertion (C6).
// Primary key: event time.  Secondary: "before halftime" flag — a first-half
// event with the same time value sorts before a second-half event.
// ---------------------------------------------------------------------------
static bool eventLess(const Event& a, const Event& b) {
    if (a.get_time() != b.get_time()) return a.get_time() < b.get_time();
    // Same time: "before halftime: true" events come first (first-half overtime)
    auto isFirst = [](const Event& e) {
        const auto& gu = e.get_game_updates();
        auto it = gu.find("before halftime");
        return it != gu.end() && it->second == "true";
    };
    return isFirst(a) && !isFirst(b);
}

// ---------------------------------------------------------------------------
// Constructor
// ---------------------------------------------------------------------------
StompProtocol::StompProtocol() :
    isConnected(false),
    subscriptionIdCounter(0),
    receiptIdCounter(0),
    topicToSubscriptionId(),
    receiptHandlers(),
    currentUser(""),
    pendingUser_(""),
    eventsHistory(),
    mtx_()
{
}

bool StompProtocol::isUserLoggedIn() {
    // No lock needed — atomic read on a bool is sufficient for the
    // listener-thread polling loop; the write is always within a lock.
    return isConnected;
}

// ---------------------------------------------------------------------------
// processUserInput
// Called from the main (keyboard) thread.
// Returns the STOMP frame(s) to send, or "" when no network action is needed.
// ---------------------------------------------------------------------------
string StompProtocol::processUserInput(string userLine) {
    lock_guard<mutex> lock(mtx_);  // C2

    stringstream ss(userLine);
    string command;
    ss >> command;

    // ------------------------------------------------------------------ LOGIN
    if (command == "login") {
        if (isConnected) {
            // C5 / spec: exact required message
            cout << "The client is already logged in, log out before trying again" << endl;
            return "";
        }
        string hostPort, username, password;
        ss >> hostPort >> username >> password;

        // C8: save as pending — only becomes currentUser on CONNECTED
        pendingUser_ = username;

        string frame = "CONNECT\n";
        frame += "accept-version:1.2\n";
        frame += "host:stomp.cs.bgu.ac.il\n";
        frame += "login:" + username + "\n";
        frame += "passcode:" + password + "\n\n";
        frame += '\0';
        return frame;

    // ------------------------------------------------------------------ JOIN
    } else if (command == "join") {
        if (!isConnected) {
            cout << "You must be logged in to join a topic." << endl;
            return "";
        }
        string topic;
        ss >> topic;
        if (topic.empty()) {
            cout << "Usage: join {game_name}" << endl;
            return "";
        }

        int subId    = subscriptionIdCounter++;
        int receiptId = receiptIdCounter++;

        topicToSubscriptionId[topic] = subId;
        receiptHandlers[receiptId]   = "Joined channel " + topic;

        string frame = "SUBSCRIBE\n";
        frame += "destination:/" + topic + "\n";
        frame += "id:" + to_string(subId) + "\n";
        frame += "receipt:" + to_string(receiptId) + "\n\n";
        frame += '\0';
        return frame;

    // ------------------------------------------------------------------ EXIT
    } else if (command == "exit") {
        if (!isConnected) {
            cout << "You must be logged in to exit a topic." << endl;
            return "";
        }
        string topic;
        ss >> topic;
        if (topic.empty()) {
            cout << "Usage: exit {game_name}" << endl;
            return "";
        }

        // C9: check that we are actually subscribed before sending UNSUBSCRIBE
        if (topicToSubscriptionId.find(topic) == topicToSubscriptionId.end()) {
            cout << "Error: not subscribed to channel " << topic << endl;
            return "";
        }

        int subId     = topicToSubscriptionId[topic];
        int receiptId = receiptIdCounter++;

        topicToSubscriptionId.erase(topic);
        receiptHandlers[receiptId] = "Exited channel " + topic;

        string frame = "UNSUBSCRIBE\n";
        frame += "id:" + to_string(subId) + "\n";
        frame += "receipt:" + to_string(receiptId) + "\n\n";
        frame += '\0';
        return frame;

    // --------------------------------------------------------------- LOGOUT
    } else if (command == "logout") {
        if (!isConnected) {
            cout << "You are not logged in." << endl;
            return "";
        }
        int receiptId = receiptIdCounter++;
        receiptHandlers[receiptId] = "DISCONNECT";

        string frame = "DISCONNECT\n";
        frame += "receipt:" + to_string(receiptId) + "\n\n";
        frame += '\0';
        return frame;

    // --------------------------------------------------------------- REPORT
    } else if (command == "report") {
        if (!isConnected) {
            cout << "You must be logged in to report." << endl;
            return "";
        }
        string file;
        ss >> file;
        if (file.empty()) {
            cout << "Usage: report {file}" << endl;
            return "";
        }

        names_and_events data;
        try {
            data = parseEventsFile(file);
        } catch (const exception& e) {
            cout << "Error parsing file: " << e.what() << endl;
            return "";
        }

        string gameName = data.team_a_name + "_" + data.team_b_name;

        // C3: Do NOT save events locally here.
        // Events arrive back via MESSAGE frames (server broadcasts to all
        // subscribers including the sender) and are stored exactly once there.

        // C11: removed DEBUG print that was left in production code.

        // Build all SEND frames (one per event)
        string totalFrames;
        for (const Event& event : data.events) {
            string frame = "SEND\n";
            frame += "destination:/" + gameName + "\n";
            frame += "filename:" + file + "\n";  // non-standard header used by server for SQL tracking
            frame += "\n";
            frame += "user: " + currentUser + "\n";
            frame += "team a: " + data.team_a_name + "\n";
            frame += "team b: " + data.team_b_name + "\n";
            frame += "event name: " + event.get_name() + "\n";
            frame += "time: " + to_string(event.get_time()) + "\n";

            frame += "general game updates:\n";
            for (auto const& p : event.get_game_updates())
                frame += "    " + p.first + ": " + p.second + "\n";

            frame += "team a updates:\n";
            for (auto const& p : event.get_team_a_updates())
                frame += "    " + p.first + ": " + p.second + "\n";

            frame += "team b updates:\n";
            for (auto const& p : event.get_team_b_updates())
                frame += "    " + p.first + ": " + p.second + "\n";

            frame += "description:\n" + event.get_discription() + "\n";
            frame += '\0';

            totalFrames += frame;
        }
        return totalFrames;

    // -------------------------------------------------------------- SUMMARY
    } else if (command == "summary") {
        string gameName, userName, file;
        ss >> gameName >> userName >> file;
        if (gameName.empty() || userName.empty() || file.empty()) {
            cout << "Usage: summary {game_name} {user} {file}" << endl;
            return "";
        }

        auto gitGame = eventsHistory.find(gameName);
        if (gitGame == eventsHistory.end()) {
            cout << "No events found for game " << gameName << endl;
            return "";
        }
        auto gitUser = gitGame->second.find(userName);
        if (gitUser == gitGame->second.end()) {
            cout << "No events found for user " << userName << " in game " << gameName << endl;
            return "";
        }

        const vector<Event>& events = gitUser->second;
        if (events.empty()) {
            cout << "No events found for user " << userName << " in game " << gameName << endl;
            return "";
        }

        // Consolidate stats (last value wins per stat name — accumulative per spec)
        map<string, string> general_stats;
        map<string, string> team_a_stats;
        map<string, string> team_b_stats;

        // Use map for lexicographic order (spec: "sorted lexicographically by their name")
        for (const auto& e : events) {
            for (const auto& p : e.get_game_updates())   general_stats[p.first] = p.second;
            for (const auto& p : e.get_team_a_updates()) team_a_stats[p.first]  = p.second;
            for (const auto& p : e.get_team_b_updates()) team_b_stats[p.first]  = p.second;
        }

        string team_a_name = events[0].get_team_a_name();
        string team_b_name = events[0].get_team_b_name();

        ofstream outFile(file);
        if (!outFile.is_open()) {
            cout << "Error opening file: " << file << endl;
            return "";
        }

        outFile << team_a_name << " vs " << team_b_name << "\n";
        outFile << "Game stats:\n";
        outFile << "General stats:\n";
        for (const auto& p : general_stats) outFile << p.first << ": " << p.second << "\n";

        outFile << "\n" << team_a_name << " stats:\n";
        for (const auto& p : team_a_stats) outFile << p.first << ": " << p.second << "\n";

        outFile << "\n" << team_b_name << " stats:\n";
        for (const auto& p : team_b_stats) outFile << p.first << ": " << p.second << "\n";

        outFile << "\nGame event reports:\n";
        // C6: events are already stored in sorted time order (sorted insert in MESSAGE handler)
        for (const auto& e : events) {
            outFile << e.get_time() << " - " << e.get_name() << ":\n";
            // C16: trim trailing whitespace/newlines from description, then output with
            //      exactly one blank line between events
            string desc = e.get_discription();
            while (!desc.empty() && (desc.back() == '\n' || desc.back() == '\r'))
                desc.pop_back();
            outFile << "\n" << desc << "\n\n";
        }

        outFile.close();
        cout << "Summary created successfully in " << file << endl;
        return "";

    // ---------------------------------------------------------------- OTHER
    } else {
        // C14: Unrecognized commands print a local error — the client must never
        //      send an ERROR frame to the server (ERROR is a server→client frame only).
        cout << "Unknown command: " << command << endl;
        return "";
    }
}

// ---------------------------------------------------------------------------
// processServerResponse
// Called from the listener thread.
// Returns a string to print to stdout, or "" for no output.
// ---------------------------------------------------------------------------
string StompProtocol::processServerResponse(string serverFrame) {
    lock_guard<mutex> lock(mtx_);  // C2

    stringstream ss(serverFrame);
    string command;
    getline(ss, command);
    if (!command.empty() && command.back() == '\r') command.pop_back();

    // ---------------------------------------------------------------- CONNECTED
    if (command == "CONNECTED") {
        isConnected = true;
        // C8: only now is the pending username confirmed
        currentUser  = pendingUser_;
        pendingUser_ = "";
        return "Login successful";
    }

    // ---------------------------------------------------------------- ERROR
    if (command == "ERROR") {
        // C5: parse the message header and map to the exact spec-required strings
        string line, messageHeader;
        while (getline(ss, line) && !line.empty() && line != "\r") {
            if (!line.empty() && line.back() == '\r') line.pop_back();
            if (line.find("message:") == 0) {
                messageHeader = line.substr(8);
            }
        }

        // Clean up connection state so the client can reconnect
        isConnected   = false;
        pendingUser_  = "";
        currentUser   = "";
        topicToSubscriptionId.clear();
        receiptHandlers.clear();

        // Map server message headers to the exact strings required by the spec
        if (messageHeader.find("Bad Credentials") != string::npos ||
            messageHeader.find("Wrong password")  != string::npos) {
            return "Wrong password";
        }
        if (messageHeader.find("already logged in") != string::npos ||
            messageHeader.find("User already logged in") != string::npos) {
            return "User already logged in";
        }
        // General error: print the message header (or full frame if header missing)
        if (!messageHeader.empty()) return "Error: " + messageHeader;
        return "Error: Server sent an ERROR frame";
    }

    // ---------------------------------------------------------------- RECEIPT
    if (command == "RECEIPT") {
        string line, receiptIdStr;
        while (getline(ss, line) && !line.empty() && line != "\r") {
            if (!line.empty() && line.back() == '\r') line.pop_back();
            if (line.find("receipt-id:") == 0) {
                receiptIdStr = line.substr(11);
            }
        }

        if (!receiptIdStr.empty()) {
            int receiptId = stoi(receiptIdStr);
            auto it = receiptHandlers.find(receiptId);
            if (it != receiptHandlers.end()) {
                string context = it->second;
                receiptHandlers.erase(it);

                if (context == "DISCONNECT") {
                    // C4: clean up state for potential reconnect; do NOT set isRunning=false
                    isConnected = false;
                    currentUser = "";
                    topicToSubscriptionId.clear();
                    receiptHandlers.clear();
                    return "Logout successful.";
                }
                return context;  // e.g. "Joined channel X" or "Exited channel X"
            }
        }
        return "";
    }

    // ---------------------------------------------------------------- MESSAGE
    if (command == "MESSAGE") {
        string destination;
        string line;

        // Parse headers
        while (getline(ss, line) && !line.empty() && line != "\r") {
            if (!line.empty() && line.back() == '\r') line.pop_back();
            if (line.find("destination:") == 0) {
                destination = line.substr(12);
            }
        }

        // Read the body
        string body;
        {
            stringstream bodyStream;
            bodyStream << ss.rdbuf();
            body = bodyStream.str();
        }

        if (destination.empty() || body.empty()) return "";

        // Strip leading '/' from destination to get the game name
        string gameName = destination;
        if (!gameName.empty() && gameName[0] == '/') gameName = gameName.substr(1);

        // Parse body fields
        stringstream bss(body);
        string user, team_a, team_b, eventName, description;
        int    eventTime = 0;
        map<string, string> general_updates, team_a_updates, team_b_updates;
        string currentSection;

        while (getline(bss, line)) {
            if (!line.empty() && line.back() == '\r') line.pop_back();
            if (line.empty()) continue;

            if (line.find("user:") == 0) {
                user = line.substr(5);
                // Trim leading space
                size_t f = user.find_first_not_of(' ');
                if (f != string::npos) user = user.substr(f);
            } else if (line.find("team a:") == 0) {
                team_a = line.substr(7);
                size_t f = team_a.find_first_not_of(' ');
                if (f != string::npos) team_a = team_a.substr(f);
            } else if (line.find("team b:") == 0) {
                team_b = line.substr(7);
                size_t f = team_b.find_first_not_of(' ');
                if (f != string::npos) team_b = team_b.substr(f);
            } else if (line.find("event name:") == 0) {
                eventName = line.substr(11);
                size_t f = eventName.find_first_not_of(' ');
                if (f != string::npos) eventName = eventName.substr(f);
            } else if (line.find("time:") == 0) {
                string tstr = line.substr(5);
                size_t f = tstr.find_first_not_of(' ');
                if (f != string::npos) tstr = tstr.substr(f);
                try { eventTime = stoi(tstr); } catch (...) { eventTime = 0; }
            } else if (line == "general game updates:") {
                currentSection = "general";
            } else if (line == "team a updates:") {
                currentSection = "team_a";
            } else if (line == "team b updates:") {
                currentSection = "team_b";
            } else if (line.find("description:") == 0) {
                currentSection = "description";
            } else {
                if (currentSection == "description") {
                    description += line + "\n";
                } else if (line.find(':') != string::npos) {
                    size_t delim = line.find(':');
                    string key = line.substr(0, delim);
                    string val = line.substr(delim + 1);
                    // Trim key and value
                    size_t kf = key.find_first_not_of(" \t");
                    if (kf != string::npos) key = key.substr(kf);
                    size_t vf = val.find_first_not_of(" \t");
                    if (vf != string::npos) val = val.substr(vf);

                    if (currentSection == "general") general_updates[key] = val;
                    else if (currentSection == "team_a") team_a_updates[key] = val;
                    else if (currentSection == "team_b") team_b_updates[key] = val;
                }
            }
        }

        if (!user.empty()) {
            // C12: argument order: (team_a, team_b, name, time, ...)
            Event receivedEvent(team_a, team_b, eventName, eventTime,
                                general_updates, team_a_updates, team_b_updates, description);

            // C3 + C6: store in eventsHistory with sorted insertion by event time.
            // This is the ONLY place events are saved — prevents duplicates and
            // guarantees ordering required by the spec.
            auto& vec = eventsHistory[gameName][user];
            auto  pos = lower_bound(vec.begin(), vec.end(), receivedEvent, eventLess);
            vec.insert(pos, receivedEvent);
        }

        // L2: do not echo the raw body; the spec does not require any stdout
        //     output when a message is received (just update internal state).
        return "";
    }

    return "";
}
