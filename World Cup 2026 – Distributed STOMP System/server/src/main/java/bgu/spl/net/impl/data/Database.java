package bgu.spl.net.impl.data;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the server's data persistence and user state.
 * Acts as a bridge between the Java server and the Python SQL backend.
 */
public class Database {
    
    private final ConcurrentHashMap<String, User> userMap;
    private final ConcurrentHashMap<Integer, User> connectionsIdMap;
    private final String sqlHost;
    private final int sqlPort;

    private Database() {
        userMap = new ConcurrentHashMap<>();
        connectionsIdMap = new ConcurrentHashMap<>();
        // SQL server connection details
        this.sqlHost = "127.0.0.1";
        this.sqlPort = 7778;

        initialize();
    }

    /**
     * Singleton instance holder.
     */
    private static class Instance {
        static final Database instance = new Database();
    }

    public static Database getInstance() {
        return Instance.instance;
    }

    /**
     * Initializes the database connection and loads existing users from the SQL server.
     * Retries connection until successful.
     */
    private void initialize() {
        System.out.println("----------------------------------------");
        System.out.println(">> Database: Connecting to Python Server...");
        
        boolean success = false;
        while (!success) {
            // Query for all users
            String response = executeSQL("SELECT username, password FROM users");
            
            if (response.startsWith("SUCCESS")) {
                String[] parts = response.split("\\|");
                // Format: SUCCESS|user1,pass1|user2,pass2
                if (parts.length > 1) {
                    for (int i = 1; i < parts.length; i++) {
                        String row = parts[i].replace("\r", "").replace("\n", "");
                        // Use regex split to handle potential spacing issues
                        String[] userParts = row.split(",\\s*"); 
                        
                        if (userParts.length >= 2) {
                            String username = userParts[0].trim();
                            String password = userParts[1].trim();
                            // Load into memory (id=-1 because they are not currently connected)
                            userMap.put(username, new User(-1, username, password));
                        }
                    }
                    System.out.println(">> Database: Loaded " + (parts.length - 1) + " users from SQL.");
                } else {
                    System.out.println(">> Database: Valid connection, but no users found (Empty DB).");
                }
                success = true; // Exit loop
                
            } else {
                // Failure case - wait and retry
                System.err.println(">> Database Error: Could not connect to Python (" + response + ")");
                System.err.println(">> Retrying in 1 second...");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        System.out.println("----------------------------------------");
    }

    /**
     * Executes a raw SQL query via the Python server bridge.
     *
     * @param sql the SQL query string
     * @return the raw result string from the SQL server
     */
    private String executeSQL(String sql) {
        try (Socket socket = new Socket(sqlHost, sqlPort);
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"))) {
             
            // Send SQL with null terminator
            out.write(sql);
            out.write('\0');
            out.flush();
            
            // Read response until null terminator
            StringBuilder response = new StringBuilder();
            int c;
            while ((c = in.read()) != -1) {
                if (c == '\0') break;
                response.append((char) c);
            }
            return response.toString();
            
        } catch (Exception e) {
            System.err.println("SQL Error: " + e.getMessage());
            return "ERROR:" + e.getMessage();
        }
    }

    /**
     * Escapes SQL special characters to prevent SQL injection (basic implementation).
     * @param str the input string
     * @return the escaped string
     */
    private String escapeSql(String str) {
        if (str == null) return "";
        return str.replace("'", "''");
    }

    /**
     * Adds a user to the in-memory maps.
     * J7: Only insert into connectionsIdMap when the user has a real connection (id != -1).
     *     Users loaded from the DB at startup have id=-1 and must not pollute the map.
     * @param user the user to add
     */
    public void addUser(User user) {
        userMap.putIfAbsent(user.name, user);
        if (user.getConnectionId() != -1) {
            connectionsIdMap.putIfAbsent(user.getConnectionId(), user);
        }
    }

    /**
     * Handles user login logic.
     * Checks credentials, updates connection ID, and logs the event to SQL.
     *
     * @param connectionId the connection ID of the client
     * @param username the username provided
     * @param password the password provided
     * @return the status of the login attempt
     */
    public LoginStatus login(int connectionId, String username, String password) {
        if (connectionsIdMap.containsKey(connectionId)) {
            return LoginStatus.CLIENT_ALREADY_CONNECTED;
        }
        if (addNewUserCase(connectionId, username, password)) {
            // Log new user registration in SQL
            String sql = String.format(
                "INSERT INTO users (username, password, registration_date) VALUES ('%s', '%s', datetime('now'))",
                escapeSql(username), escapeSql(password)
            );
            executeSQL(sql);
            
            // Log login
            logLogin(username);
            return LoginStatus.ADDED_NEW_USER;
        } else {
            LoginStatus status = userExistsCase(connectionId, username, password);
            if (status == LoginStatus.LOGGED_IN_SUCCESSFULLY) {
                // Log successful login in SQL
                logLogin(username);
            }
            return status;
        }
    }

    /**
     * Helper to log a login event to the SQL database.
     */
    private void logLogin(String username) {
        String sql = String.format(
            "INSERT INTO login_history (username, login_time) VALUES ('%s', datetime('now'))",
            escapeSql(username)
        );
        executeSQL(sql);
    }

    /**
     * Handles logic for existing users attempting to login.
     * J1: Check password BEFORE checking isLoggedIn — wrong-password errors must not
     *     be swallowed when the user happens to be logged in elsewhere.
     */
    private LoginStatus userExistsCase(int connectionId, String username, String password) {
        User user = userMap.get(username);
        synchronized (user) {
            if (!user.password.equals(password)) {
                return LoginStatus.WRONG_PASSWORD;
            } else if (user.isLoggedIn()) {
                return LoginStatus.ALREADY_LOGGED_IN;
            } else {
                user.login();
                user.setConnectionId(connectionId);
                connectionsIdMap.put(connectionId, user);
                return LoginStatus.LOGGED_IN_SUCCESSFULLY;
            }
        }
    }

    /**
     * Handles logic for new user registration.
     */
    private boolean addNewUserCase(int connectionId, String username, String password) {
        if (!userMap.containsKey(username)) {
            synchronized (userMap) {
                if (!userMap.containsKey(username)) {
                    User user = new User(connectionId, username, password);
                    user.login();
                    addUser(user);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Handles user logout.
     * Removes from active connections map and updates logout time in SQL.
     *
     * @param connectionsId the connection ID to log out
     */
    public void logout(int connectionsId) {
        User user = connectionsIdMap.get(connectionsId);
        if (user != null) {
            // J9: Standard SQLite does not support UPDATE ... ORDER BY ... LIMIT.
            //     Use a subquery that selects the rowid of the most recent open session.
            String sql = String.format(
                "UPDATE login_history SET logout_time=datetime('now') " +
                "WHERE rowid = (SELECT rowid FROM login_history " +
                "WHERE username='%s' AND logout_time IS NULL " +
                "ORDER BY login_time DESC LIMIT 1)",
                escapeSql(user.name)
            );
            executeSQL(sql);
            
            user.logout();
            connectionsIdMap.remove(connectionsId);
        }
    }

    /**
     * Tracks a file upload event in the SQL database.
     *
     * @param username User who uploaded the file
     * @param filename Name of the file
     * @param gameChannel Game channel the file was reported to
     */
    public void trackFileUpload(String username, String filename, String gameChannel) {
        String sql = String.format(
            "INSERT OR IGNORE INTO file_tracking (username, filename, upload_time, game_channel) " +
            "VALUES ('%s', '%s', datetime('now'), '%s')",
            escapeSql(username), escapeSql(filename), escapeSql(gameChannel)
        );
        executeSQL(sql);
    }

    /**
     * Generates and prints a comprehensive server report using SQL queries.
     * Fetches real-time data from the SQL backend regarding users, login history, and files.
     */
    public void printReport() {
        System.out.println(repeat("=", 80));
        System.out.println("SERVER REPORT - Generated at: " + java.time.LocalDateTime.now());
        System.out.println(repeat("=", 80));
        
        // 1. List all users
        System.out.println("\n1. REGISTERED USERS:");
        System.out.println(repeat("-", 80));
        String usersSQL = "SELECT username, registration_date FROM users ORDER BY registration_date";
        String usersResult = executeSQL(usersSQL);
        if (usersResult.startsWith("SUCCESS")) {
            String[] parts = usersResult.split("\\|");
            if (parts.length > 1) {
                for (int i = 1; i < parts.length; i++) {
                    System.out.println("   " + parts[i]);
                }
            } else {
                System.out.println("   No users registered");
            }
        }
        
        // 2. Login history
        System.out.println("\n2. LOGIN HISTORY:");
        System.out.println(repeat("-", 80));
        String loginSQL = "SELECT username, login_time, logout_time FROM login_history ORDER BY username, login_time DESC";
        String loginResult = executeSQL(loginSQL);
        if (loginResult.startsWith("SUCCESS")) {
            String[] parts = loginResult.split("\\|");
            if (parts.length > 1) {
                String currentUser = "";
                for (int i = 1; i < parts.length; i++) {
                    String[] fields = parts[i].replace("(", "").replace(")", "").replace("'", "").split(", ");
                    if (fields.length >= 3) {
                        if (!fields[0].equals(currentUser)) {
                            currentUser = fields[0];
                            System.out.println("\n   User: " + currentUser);
                        }
                        System.out.println("      Login:  " + fields[1]);
                        System.out.println("      Logout: " + (fields[2].equals("None") ? "Still logged in" : fields[2]));
                    }
                }
            } else {
                System.out.println("   No login history");
            }
        }
        
        // 3. File uploads
        System.out.println("\n3. FILE UPLOADS:");
        System.out.println(repeat("-", 80));
        String filesSQL = "SELECT username, filename, upload_time, game_channel FROM file_tracking ORDER BY username, upload_time DESC";
        String filesResult = executeSQL(filesSQL);
        if (filesResult.startsWith("SUCCESS")) {
            String[] parts = filesResult.split("\\|");
            if (parts.length > 1) {
                String currentUser = "";
                for (int i = 1; i < parts.length; i++) {
                    String[] fields = parts[i].replace("(", "").replace(")", "").replace("'", "").split(", ");
                    if (fields.length >= 4) {
                        if (!fields[0].equals(currentUser)) {
                            currentUser = fields[0];
                            System.out.println("\n   User: " + currentUser);
                        }
                        System.out.println("      File: " + fields[1]);
                        System.out.println("      Time: " + fields[2]);
                        System.out.println("      Game: " + fields[3]);
                        System.out.println();
                    }
                }
            } else {
                System.out.println("   No files uploaded");
            }
        }
        
        System.out.println(repeat("=", 80));
    }

    private String repeat(String str, int times) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < times; i++) {
            sb.append(str);
        }
        return sb.toString();
    }
}