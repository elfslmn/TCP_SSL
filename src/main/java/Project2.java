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
        String mode = "";
        while(!mode.equals("s") && !mode.equals("c")) {
            System.out.println("Write s for Server , c for Client");
            mode = scanner.nextLine();
        }

        String conType = "";
        while(!conType.equals("s") && !conType.equals("t")) {
            System.out.println("Write t for TCP , s for SSL");
            conType = scanner.nextLine();
        }

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
            }
        }



        // TODO close connections and save current map to txt.
        if(conType.equals("t")){
// TCP Server ----------------------------------------------------------------------------------------------------------------
            if(mode.equals("s")){
                try {
                    ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
                    System.out.println("Waiting connections...");
                    while(true){
                        Socket socket = serverSocket.accept();
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
                    Socket socket =new Socket(SERVER_ADDRESS, SERVER_PORT);
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
                    KeyStore keyStore = KeyStore.getInstance("JKS");
                    char ksPass[] = SERVER_KEYSTORE_PASSWORD.toCharArray();
                    keyStore.load(new FileInputStream(SERVER_KEYSTORE_FILE), ksPass);
                    KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
                    keyManagerFactory.init(keyStore, SERVER_KEY_PASSWORD.toCharArray());
                    sslContext.init(keyManagerFactory.getKeyManagers(), null, null);

                    sslServerSocketFactory = sslContext.getServerSocketFactory();
                    sslServerSocket = (SSLServerSocket) sslServerSocketFactory.createServerSocket(SERVER_PORT);
                    System.out.println("Waiting connections...");

                    while(true) {
                        SSLSocket socket = (SSLSocket) sslServerSocket.accept();
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
    public static void insert2Db(Connection conn, String key, String value){
        String sql;
        try {
            // Check key exists already
            sql = "SELECT key, value FROM pairs WHERE key = \""+key+"\"";
            Statement stmt  = conn.createStatement();
            ResultSet rs    = stmt.executeQuery(sql);
            if(rs.next()){
                if( rs.getString("value").equals(value) ){
                    System.out.format("Existing entry: <%s,%s>, already saved.\n", key, value);
                    return;
                }
                sql = "UPDATE pairs SET value = ? WHERE key = ?";
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setString(1,value);
                pstmt.setString(2,key);
                pstmt.executeUpdate();
                System.out.format("Value for key <%s> updated as (%s) -> (%s)\n", key, rs.getString("value"), value);
            }
            else{
                sql = "INSERT INTO pairs(key,value) VALUES(?,?)";
                PreparedStatement pstmt = conn.prepareStatement(sql);
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

    public static String queryDb(Connection conn, String key){
        String sql = "SELECT key, value FROM pairs WHERE key = \""+key+"\"";
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs    = stmt.executeQuery(sql);
            if(rs.next()){
                return rs.getString("value");
            }
            else{
                return null;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
}
