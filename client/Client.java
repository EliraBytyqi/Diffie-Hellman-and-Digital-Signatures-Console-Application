package client;

import crypto.*;

import javax.crypto.SecretKey;
import javax.crypto.spec.DHParameterSpec;
import java.io.*;
import java.net.Socket;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;
import java.util.Scanner;

public class Client {

    private static final String HOST = "localhost";
    private static final int PORT = 5000;

    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;
    private Scanner scanner;
    private SecretKey aesKey;
    private PublicKey serverPublicKey;

    public Client() throws IOException {
        socket = new Socket(HOST, PORT);
        input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        output = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
        scanner = new Scanner(System.in);
    }

    public static void main(String[] args) {
        try {
            Client client = new Client();
            client.run();
        } catch (Exception e) {
            System.err.println("Client Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void run() throws Exception {
        System.out.println("=================================");
        System.out.println(" Secure Chat Client");
        System.out.println("=================================");
        System.out.println("Connecting to server at " + HOST + ":" + PORT);

        try {
            // Hapi 1: Kryej shkëmbimin e çelësave Diffie-Hellman
            performDiffieHellmanExchange();

            // Hapi 2: Prano dhe verifiko mesazhin e nënshkruar nga serveri
            receiveAndVerifySignedMessage();

            // Hapi 3: Fillo komunikimin interaktiv të enkriptuar
            handleEncryptedCommunication();

        } finally {
            cleanup();
        }
    }

    private void performDiffieHellmanExchange() throws Exception {
        System.out.println("\nPerforming Diffie-Hellman key exchange...");

        // Prano parametrat DH nga serveri
        String dhParamsMessage = input.readLine();
        if (dhParamsMessage == null || !dhParamsMessage.startsWith("DH_PARAMS:")) {
            throw new Exception("Invalid DH parameters message from server");
        }

        String encodedDHParams = dhParamsMessage.substring("DH_PARAMS:".length());
        DHParameterSpec dhParams = DiffieHellmanUtil.decodeDhParameters(encodedDHParams);

        System.out.println("✓ Received DH parameters from server");

        // Prano çelësin publik DH të serverit
        String serverKeyMessage = input.readLine();
        if (serverKeyMessage == null || !serverKeyMessage.startsWith("DH_PUBLIC_KEY:")) {
            throw new Exception("Invalid server DH public key message");
        }

        String encodedServerPublicKey = serverKeyMessage.substring("DH_PUBLIC_KEY:".length());
        PublicKey serverDHPublicKey = DiffieHellmanUtil.decodePublicKey(encodedServerPublicKey);

        System.out.println("✓ Received server's DH public key");

        // Gjenero çiftin e çelësave DH të klientit
        KeyPair clientDHKeyPair = DiffieHellmanUtil.generateKeyPair(dhParams);
        PublicKey clientDHPublicKey = clientDHKeyPair.getPublic();
        PrivateKey clientDHPrivateKey = clientDHKeyPair.getPrivate();

        System.out.println("✓ Generated client's DH key pair");

        // Dërgo çelësin publik DH të klientit te serveri
        String encodedClientPublicKey = DiffieHellmanUtil.encodePublicKey(clientDHPublicKey);
        output.println("CLIENT_DH_PUBLIC_KEY:" + encodedClientPublicKey);

        System.out.println("✓ Sent client's DH public key to server");

        // Llogarit sekretin e përbashkët dhe nxirr çelësin AES
        aesKey = DiffieHellmanUtil.calculateAesKey(clientDHPrivateKey, serverDHPublicKey);

        System.out.println("✓ Shared secret established and AES key derived");

        // Prit konfirmimin nga serveri
        String ackMessage = input.readLine();
        if (ackMessage != null && ackMessage.startsWith("ACK:DH_COMPLETE")) {
            System.out.println("✓ Key exchange completed successfully\n");
        }
    }

    private void receiveAndVerifySignedMessage() throws Exception {
        System.out.println("Receiving and verifying server's credentials...");

        // Prano çelësin publik të serverit
        String publicKeyMessage = input.readLine();
        if (publicKeyMessage == null || !publicKeyMessage.startsWith("SERVER_PUBLIC_KEY:")) {
            throw new Exception("Invalid server public key message");
        }

        String encodedPublicKey = publicKeyMessage.substring("SERVER_PUBLIC_KEY:".length());
        serverPublicKey = decodePublicKeyFromString(encodedPublicKey);

        System.out.println("✓ Received server's RSA public key");

        // Prano mesazhin e mirëseardhjes të nënshkruar
        String signedMessageLine = input.readLine();
        if (signedMessageLine == null || !signedMessageLine.startsWith("SIGNED_MESSAGE:")) {
            throw new Exception("Invalid signed message format");
        }

        String signedMessageData = signedMessageLine.substring("SIGNED_MESSAGE:".length());
        String[] parts = signedMessageData.split("\\|");
        if (parts.length != 2) {
            throw new Exception("Invalid signed message format");
        }

        String encryptedMessage = parts[0];
        String signature = parts[1];

        // Dekripto mesazhin
        String welcomeMessage = AESUtil.decrypt(encryptedMessage, aesKey);

        System.out.println("✓ Received encrypted welcome message");

        // Verifiko nënshkrimin
        boolean isSignatureValid = SignatureUtil.verifySignature(
                welcomeMessage,
                signature,
                serverPublicKey
        );

        if (isSignatureValid) {
            System.out.println("✓ Signature verified successfully!");
            System.out.println("✓ Server is authentic and trustworthy\n");
            System.out.println("Server Message: " + welcomeMessage);
            output.println("SIGNATURE_VERIFIED");
        } else {
            throw new Exception("Signature verification FAILED! Server identity cannot be trusted.");
        }
    }

    private void handleEncryptedCommunication() throws Exception {
        System.out.println("\n=================================");
        System.out.println(" Encrypted Communication Mode");
        System.out.println("=================================");
        System.out.println("Type 'exit' to disconnect\n");

        while (true) {
            System.out.print("You: ");
            String message = scanner.nextLine();

            if (message.equalsIgnoreCase("exit")) {
                System.out.println("Disconnecting...");
                break;
            }

            if (message.trim().isEmpty()) {
                continue;
            }

            try {
                // Krijo hash-in e mesazhit për integritet
                String messageHash = HashUtil.sha256(message);

                // Kripto mesazhin
                String encryptedMessage = AESUtil.encrypt(message, aesKey);

                // Dërgo mesazhin e kriptuar
                output.println(encryptedMessage);

                System.out.println("[Sent - Hash: " + messageHash.substring(0, 16) + "...]");

                // Prano përgjigjen e kriptuar
                String encryptedResponse = input.readLine();

                if (encryptedResponse == null) {
                    System.out.println("Server disconnected.");
                    break;
                }

                // Analizo përgjigjen dhe nënshkrimin
                String[] responseParts = encryptedResponse.split("\\|");
                String decryptedResponse = AESUtil.decrypt(responseParts[0], aesKey);

                if (responseParts.length > 1) {
                    String responseSignature = responseParts[1];

                    // Verifiko nënshkrimin e përgjigjes
                    boolean signatureValid = SignatureUtil.verifySignature(
                            decryptedResponse,
                            responseSignature,
                            serverPublicKey
                    );

                    if (signatureValid) {
                        System.out.println("Server: " + decryptedResponse + " [✓ Signature Verified]");
                    } else {
                        System.out.println("Server: " + decryptedResponse + " [⚠ Signature NOT verified!]");
                    }
                } else {
                    System.out.println("Server: " + decryptedResponse);
                }

            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
        }
    }

    private PublicKey decodePublicKeyFromString(String encodedKey) throws Exception {
        byte[] decodedKey = Base64.getDecoder().decode(encodedKey);
        java.security.spec.X509EncodedKeySpec spec =
                new java.security.spec.X509EncodedKeySpec(decodedKey);
        java.security.KeyFactory kf = java.security.KeyFactory.getInstance("RSA");
        return kf.generatePublic(spec);
    }

    private void cleanup() {
        try {
            if (scanner != null) scanner.close();
            if (input != null) input.close();
            if (output != null) output.close();
            if (socket != null) socket.close();
            System.out.println("Disconnected.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}