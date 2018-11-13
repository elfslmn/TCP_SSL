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

                                String message, command, key, value;
                                while(true){
                                    message = inStream.readLine().trim();
                                    System.out.println("Client: "+message);
                                    command = message.substring(0,message.indexOf(' ')).trim();
                                    if(command.equals("submit") && message.contains(",")){
                                        key = message.substring(message.indexOf(' '),message.indexOf(',')).trim();
                                        value =  message.substring(message.indexOf(',')+1,message.length()).trim();
                                        map.put(key, value);
                                        System.out.format("<%s,%s> saved, map size=%d\n",key,value,map.size());
                                        outStream.println("OK");
                                        outStream.flush();
                                    }
                                    else if(command.equals("get")){
                                        key = message.substring(message.indexOf(' '),message.length()).trim();
                                        if(map.containsKey(key)){
                                            value = map.get(key);
                                            outStream.println(value);
                                            outStream.flush();
                                            System.out.format("Entry found <%s,%s>\n",key, value );
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

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }

    }
}
