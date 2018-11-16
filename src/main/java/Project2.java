import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.security.KeyStore;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Scanner;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * Created by esalman17 on 12.11.2018.
 */

public class Project2 {
    public static Scanner scanner = new Scanner(System.in);
    public static final int SERVER_PORT = 4445;
    public static final String SERVER_ADDRESS = "localhost";
    public static final String DATABASE_PATH = "C:/Users/esalman17/Desktop/TCP_SSL/sqlite/map.db";

    static Connection connection = null;

    public static void main(String[] arg){
        // 1- Get operation mode
        String mode = "";
        while(!mode.equals("s") && !mode.equals("c")) {
            System.out.println("Write s for Server , c for Client");
            mode = scanner.nextLine();
        }

        // 2- Get connection type
        String conType = "";
        while(!conType.equals("s") && !conType.equals("t")) {
            System.out.println("Write t for TCP , s for SSL");
            conType = scanner.nextLine();
        }

        // 3- If it is server, connect to database (create if it does not exist)
        if(mode.equals("s")){
            try {
                connection = DriverManager.getConnection("jdbc:sqlite:"+DATABASE_PATH);
                if(connection == null){
                    System.out.println("Database connection is failed!");
                    return;
                }
                String sql = "CREATE TABLE IF NOT EXISTS pairs (\n"
                        + "	key text PRIMARY KEY,\n"
                        + "	value text NOT NULL\n"
                        + ");";
                Statement stmt = connection.createStatement();
                stmt.execute(sql);

            } catch (SQLException e) {
                e.printStackTrace();
                System.out.println("Database connection is failed!");
                return;
            }
        }


        if(conType.equals("t")){
// TCP Server ----------------------------------------------------------------------------------------------------------------
            if(mode.equals("s")){
                try {
                    // Open a server socket
                    ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
                    System.out.println("Waiting connections...");
                    while(true){
                        Socket socket = serverSocket.accept();
                        // Upon new connection, start a new thread to serve to the connected client
                        new Thread(() -> {
                            System.out.println("New connection established.");
                            try {
                                BufferedReader inStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                                PrintWriter outStream = new PrintWriter(socket.getOutputStream());
                               serverLoop(inStream, outStream);

                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                        }).start();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

// TCP Client ---------------------------------------------------------------------------------------
            else if(mode.equals("c")){
                try {
                    // Create a socket to connect to server.
                    Socket socket =new Socket(SERVER_ADDRESS, SERVER_PORT);
                    // Set timeout to 2sec for resending
                    socket.setSoTimeout(2000);

                    BufferedReader inStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    PrintWriter outStream = new PrintWriter(socket.getOutputStream());
                    clientLoop(inStream, outStream);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }

        else if(conType.equals("s")){
// SSL Server ----------------------------------------------------------------------------------------------------------------
            final String SERVER_KEYSTORE_FILE = "keystore.jks";
            final String SERVER_KEYSTORE_PASSWORD = "storepass";
            final String SERVER_KEY_PASSWORD = "keypass";

            if(mode.equals("s")){
                SSLServerSocket sslServerSocket;
                SSLServerSocketFactory sslServerSocketFactory;

                try {
                    SSLContext sslContext = SSLContext.getInstance("TLS");
                    // Set key
                    KeyStore keyStore = KeyStore.getInstance("JKS");
                    char ksPass[] = SERVER_KEYSTORE_PASSWORD.toCharArray();
                    keyStore.load(new FileInputStream(SERVER_KEYSTORE_FILE), ksPass);
                    KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
                    keyManagerFactory.init(keyStore, SERVER_KEY_PASSWORD.toCharArray());
                    sslContext.init(keyManagerFactory.getKeyManagers(), null, null);

                    // Open a secure server socket
                    sslServerSocketFactory = sslContext.getServerSocketFactory();
                    sslServerSocket = (SSLServerSocket) sslServerSocketFactory.createServerSocket(SERVER_PORT);
                    System.out.println("Waiting connections...");

                    while(true) {
                        SSLSocket socket = (SSLSocket) sslServerSocket.accept();
                        // Upon new connection, start a new thread to serve to the connected client
                        new Thread(() -> {
                            System.out.println("New SSL connection established.");
                            try {
                                BufferedReader inStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                                PrintWriter outStream = new PrintWriter(socket.getOutputStream());
                                serverLoop(inStream, outStream);

                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }).start();
                    }


                } catch (Exception e) {
                    e.printStackTrace();
                }

            }

// SSL Client ----------------------------------------------------------------------------------------------------------------
            else if(mode.equals("c")){
                final String KEY_STORE_NAME =  "clientkeystore";
                final String KEY_STORE_PASSWORD = "storepass";
                System.setProperty("javax.net.ssl.trustStore", KEY_STORE_NAME);
                System.setProperty("javax.net.ssl.trustStorePassword", KEY_STORE_PASSWORD);

                try {
                    // Validate keys of client and server
                    SSLSocketFactory sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
                    SSLSocket sslSocket = (SSLSocket) sslSocketFactory.createSocket(SERVER_ADDRESS, SERVER_PORT);
                    sslSocket.startHandshake();

                    BufferedReader inStream = new BufferedReader(new InputStreamReader(sslSocket.getInputStream()));
                    PrintWriter outStream = new PrintWriter(sslSocket.getOutputStream());
                    clientLoop(inStream, outStream);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

    }


// Tasks --------------------------------------------------------------------------------------------------------------------

    /**
     * The function that implements the jobs of the server such that:
     * Listens the client
     * Upon "submit" command, saves given pair to database
     * Upon "get", retrieve the value with specified key
     *
     * @param inStream input stream of the client socket
     * @param outStream output stream of the client socket
     * @throws IOException
     */
    private static void serverLoop(BufferedReader inStream, PrintWriter outStream) throws IOException {
        String message, command, key, value;
        while(true){
            message = inStream.readLine().trim();
            System.out.println("Client: "+message);
            command = message.substring(0,message.indexOf(' ')).trim();
            if(command.equals("submit") && message.contains(",")){
                key = message.substring(message.indexOf(' '),message.indexOf(',')).trim();
                value =  message.substring(message.indexOf(',')+1,message.length()).trim();
                insert2Db(connection, key, value);
                outStream.println("OK");
                outStream.flush();
            }
            else if(command.equals("get")){
                key = message.substring(message.indexOf(' '),message.length()).trim();
                value = queryDb(connection, key);
                if(value == null){
                    outStream.println("No stored value for " + key);
                    outStream.flush();
                    System.out.println("No stored value for " + key);
                }
                else{
                    outStream.println(value);
                    outStream.flush();
                    System.out.format("Entry found <%s,%s>\n",key, value );
                }
            }
            else{
                System.out.println("Client message has wrong format: " +message);
            }
        }
    }

    /**
     * The function that implements the jobs of the server such that:
     * Listens the user
     * Upon "submit" and "get" command, send the specified arguments to server
     * @param inStream input stream of the client socket
     * @param outStream output stream of the client socket
     * @throws IOException
     */
    private static void clientLoop(BufferedReader inStream, PrintWriter outStream) throws IOException {
        String message, response, command, key, value;
        while (true){
            System.out.print("Type:");
            message = scanner.nextLine().trim();
            command = message.substring(0,message.indexOf(' ')).trim();
            if(!command.equals("submit") && !command.equals("get")){
                System.out.print("Error: wrong command format: " + command);
                continue;
            }
            if(command.equals("submit") && !message.contains(",")){
                System.out.println("Use comma between key and value that you submit");
                continue;
            }

            while(true) {
                outStream.println(message);
                outStream.flush();
                try {
                    response = inStream.readLine();
                    if (command.equals("submit") && response.equals("OK")) {
                        key = message.substring(message.indexOf(' '),message.indexOf(',')).trim();
                        value =  message.substring(message.indexOf(',')+1,message.length()).trim();
                        System.out.format("Successfully submitted <%s,%s> to server at IP address of %s\n"
                                , key, value, SERVER_ADDRESS);
                        break;
                    }
                    else if(command.equals("get")){
                        System.out.println("Server: "+response);
                        break;
                    }
                } catch (SocketTimeoutException e) {
                    System.out.println("Timeout: Resending");
                }
            }
        }
    }

    /**
     *  Insert the given key value pair to the database.
     *  If key already exists, updates the value.
     * @param connection Database connection
     * @param key key
     * @param value value
     */
    public static void insert2Db(Connection connection, String key, String value){
        String sql;
        try {
            // Check if key exists already
            sql = "SELECT key, value FROM pairs WHERE key = \""+key+"\"";
            Statement stmt  = connection.createStatement();
            ResultSet rs    = stmt.executeQuery(sql);
            // Update the value if key already exists
            if(rs.next()){
                if( rs.getString("value").equals(value) ){
                    System.out.format("Existing entry: <%s,%s>, already saved.\n", key, value);
                    return;
                }
                sql = "UPDATE pairs SET value = ? WHERE key = ?";
                PreparedStatement pstmt = connection.prepareStatement(sql);
                pstmt.setString(1,value);
                pstmt.setString(2,key);
                pstmt.executeUpdate();
                System.out.format("Value for key <%s> updated as (%s) -> (%s)\n", key, rs.getString("value"), value);
            }
            // Insert the key,value pair if key does not exists
            else{
                sql = "INSERT INTO pairs(key,value) VALUES(?,?)";
                PreparedStatement pstmt = connection.prepareStatement(sql);
                pstmt.setString(1, key);
                pstmt.setString(2, value);
                pstmt.executeUpdate();
                System.out.format("New entry: <%s,%s> saved.");
            }

        } catch (SQLException e) {
            System.out.println("Insertion failed!");
            e.printStackTrace();
        }
    }

    /**
     * Retrieve the value with specified key.
     * Return null if the key does not exist in database
     * @param connection  Database connection
     * @param key key
     * @return
     */
    public static String queryDb(Connection connection, String key){
        String sql = "SELECT key, value FROM pairs WHERE key = \""+key+"\"";
        try {
            Statement stmt = connection.createStatement();
            ResultSet rs    = stmt.executeQuery(sql);
            //Return the value, if key exists
            if(rs.next()){
                return rs.getString("value");
            }
            // Return null, if key does not exists
            else{
                return null;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
}
