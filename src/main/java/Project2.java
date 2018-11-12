import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;

/**
 * Created by esalman17 on 12.11.2018.
 */

public class Project2 {
    public static Scanner scanner = new Scanner(System.in);
    public static final int SERVER_PORT = 4444;
    public static final String SERVER_ADDRESS = "localhost";

    public static void main(String[] arg){
        System.out.println("Write s for Server , c for Client");
        String mode = scanner.nextLine();

        System.out.println("Write t for TCP , s for SSL");
        String conType = scanner.nextLine();

// TCP -----------------------------------------------------------------------------------
        if(conType.equals("t")){
            // Server
            if(mode.equals("s")){
                try {
                    ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
                    System.out.println("Waiting connections...");
                    HashMap<String, String> map = new HashMap<>();
                    while(true){
                        Socket socket = serverSocket.accept();
                        new Thread(() -> {
                            System.out.println("New connection established.");
                            try {
                                BufferedReader inStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                                PrintWriter outStream = new PrintWriter(socket.getOutputStream());

                                String message;
                                while(true){
                                    message = inStream.readLine().trim();
                                    String parts[] = message.split("\\s*(,|\\s)\\s*");
                                    System.out.println("Client: "+message);
                                    if(parts.length>0 && parts[0].equals("submit") && message.contains(",")){
                                        map.put(parts[1], parts[2]);
                                        System.out.println("entry saved, map size="+map.size());
                                        outStream.println("OK");
                                        outStream.flush();
                                    }
                                    else if(parts.length>0 && parts[0].equals("get")){
                                        String key = message.substring(message.indexOf(' '),message.length());
                                        if(map.containsKey(key)){
                                            String value = map.get(key);
                                            outStream.println(value);
                                            outStream.flush();
                                            System.out.format("Entry found for key %s -> %s\n",key, value );
                                        }
                                        else{
                                            outStream.println("No stored value for " + key);
                                            outStream.flush();
                                            System.out.println("No stored value for " + key);
                                        }
                                    }
                                    else{
                                        System.out.println("Client message has wrong format: " +message);
                                    }
                                }


                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                        }).start();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            // Client
            else if(mode.equals("c")){
                try {
                    Socket socket =new Socket(SERVER_ADDRESS, SERVER_PORT);
                    socket.setSoTimeout(2000);
                    BufferedReader inStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    PrintWriter outStream = new PrintWriter(socket.getOutputStream());

                    String message, response;
                    while (true){
                        message = scanner.nextLine().trim();
                        String parts[] = message.split("\\s*(,|\\s)\\s*");
                        if(!parts[0].equals("submit") && !parts[0].equals("get")){
                            System.out.print("Error: wrong command format: " + parts.length);
                            System.out.println(Arrays.asList(parts).toString());
                            continue;
                        }
                        if(parts[0].equals("submit") && !message.contains(",")){
                            System.out.println("Use comma between key and value that you submit");
                            continue;
                        }

                        while(true) {
                            outStream.println(message);
                            outStream.flush();
                            try {
                                response = inStream.readLine();
                                if (parts[0].equals("submit") && response.equals("OK")) {
                                    System.out.format("Successfully submitted <%s, %s> to server at IP address of %s\n"
                                            , parts[1], parts[2], SERVER_ADDRESS);
                                    break;
                                }
                                else if(parts[0].equals("get")){
                                    System.out.println("Server: "+response);
                                    break;
                                }
                            } catch (SocketTimeoutException e) {
                                System.out.println("Timeout: Resending");
                            }
                        }
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }

    }
}
