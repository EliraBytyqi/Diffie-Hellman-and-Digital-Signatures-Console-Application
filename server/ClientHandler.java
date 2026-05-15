import java.io.*;
import java.net.*;

public class ClientHandler implements Runnable {

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {

        try {

            in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream())
            );

            out = new PrintWriter(
                    socket.getOutputStream(), true
            );

            out.println("Connected to server");

            String msg;

            while ((msg = in.readLine()) != null) {

                System.out.println("Client: " + msg);

                out.println("Server received: " + msg);

                // Future integration
                // if msg.startsWith("DH:")
                // if msg.startsWith("SIG:")
                // if msg.startsWith("AES:")
            }

        } catch (Exception e) {

            System.out.println("Client disconnected");

        } finally {

            try {
                socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}