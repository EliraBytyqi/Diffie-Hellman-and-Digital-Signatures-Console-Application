package client;

import crypto.AESUtil;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class Client {

    public static void main(String[] args) {

        String host = "localhost";
        int port = 5000;

        try (
                Socket socket = new Socket(host, port);
                BufferedReader input = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
                PrintWriter output = new PrintWriter(socket.getOutputStream(), true);
                Scanner scanner = new Scanner(System.in)
        ) {

            System.out.println("=================================");
            System.out.println(" Secure Chat Client");
            System.out.println("=================================");
            System.out.println("Connected to server.");

            /*
             TEMPORARY AES KEY
             Later this comes from Diffie-Hellman
            */
            byte[] keyBytes = "1234567890123456".getBytes(StandardCharsets.UTF_8);

            SecretKey aesKey =
                    new SecretKeySpec(keyBytes, "AES");

            while (true) {

                System.out.print("You: ");
                String message = scanner.nextLine();

                if (message.equalsIgnoreCase("exit")) {
                    break;
                }

                String encryptedMessage =
                        AESUtil.encrypt(message, aesKey);

                output.println(encryptedMessage);

                System.out.println("Encrypted: " + encryptedMessage);

                String encryptedResponse = input.readLine();

                if (encryptedResponse == null) {
                    break;
                }

                String decryptedResponse =
                        AESUtil.decrypt(encryptedResponse, aesKey);

                System.out.println("Server: " + decryptedResponse);
            }

            System.out.println("Disconnected.");

        } catch (Exception e) {
            System.out.println("Client Error:");
            e.printStackTrace();
        }
    }
}