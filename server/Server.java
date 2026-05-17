package server;

import java.net.*;

public class Server {

    private static final int PORT = 5000;

    public static void main(String[] args) {

        try {
            ServerSocket serverSocket = new ServerSocket(PORT);
            
            System.out.println("=================================");
            System.out.println(" Secure Server");
            System.out.println("=================================");
            System.out.println("Listening for connections on port " + PORT + "...");

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("\nClient connected: " + socket.getInetAddress().getHostAddress());

                ClientHandler handler = new ClientHandler(socket);
                Thread thread = new Thread(handler);
                thread.setName("ClientHandler-" + socket.getInetAddress().getHostAddress());
                thread.start();
            }

        } catch (Exception e) {
            System.err.println("Server Error:");
            e.printStackTrace();
        }
    }
}