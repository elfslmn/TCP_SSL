import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
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
        System.out.println("Write 0 for Server , 1 for Client");
        int mode = scanner.nextInt();

        System.out.println("Write 0 for TCP , 1 for SSL");
        int conType = scanner.nextInt();

// TCP -----------------------------------------------------------------------------------
        if(conType == 0){
            // Server
            if(mode == 0){

            }

            // Client
            else if(mode == 1){


        }

    }
}
