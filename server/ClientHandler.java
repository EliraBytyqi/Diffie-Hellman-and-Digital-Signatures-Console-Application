package server;

import crypto.*;

import javax.crypto.SecretKey;
import javax.crypto.spec.DHParameterSpec;
import java.io.*;
import java.net.*;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

public class ClientHandler implements Runnable {

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private PrivateKey serverPrivateKey;
    private PublicKey serverPublicKey;
    private SecretKey aesKey;
    private String clientAddress;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        this.clientAddress = socket.getInetAddress().getHostAddress();
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);

            System.out.println("[" + clientAddress + "] Connected");

            // Hapi 1: Gjenero çiftin e çelësave RSA për nënshkrime digjitale
            System.out.println("[" + clientAddress + "] Generating RSA key pair for signatures...");
            KeyPair rsaKeyPair = SignatureUtil.generateKeyPair();
            serverPrivateKey = rsaKeyPair.getPrivate();
            serverPublicKey = rsaKeyPair.getPublic();

            // Hapi 2: Kryej shkëmbimin e çelësave Diffie-Hellman
            performDiffieHellmanExchange();

            // Hapi 3: Dërgo mesazhin e mirëseardhjes të nënshkruar
            sendSignedWelcomeMessage();

            // Hapi 4: Trajto komunikimin e enkriptuar
            handleEncryptedCommunication();

        } catch (Exception e) {
            System.err.println("[" + clientAddress + "] Error: " + e.getMessage());
        } finally {
            try {
                socket.close();
                System.out.println("[" + clientAddress + "] Disconnected");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void performDiffieHellmanExchange() throws Exception {
        System.out.println("[" + clientAddress + "] Starting Diffie-Hellman key exchange...");

        // Gjenero parametrat DH (numër i madh prim dhe themel)
        DHParameterSpec dhParams = DiffieHellmanUtil.generateParameters();
        System.out.println("[" + clientAddress + "] DH parameters generated");

        // Gjenero çiftin e çelësave DH të serverit
        KeyPair serverDHKeyPair = DiffieHellmanUtil.generateKeyPair(dhParams);
        PublicKey serverDHPublicKey = serverDHKeyPair.getPublic();
        PrivateKey serverDHPrivateKey = serverDHKeyPair.getPrivate();

        // Dërgo parametrat DH dhe çelësin publik të serverit te klienti
        String encodedDHParams = DiffieHellmanUtil.encodeDhParameters(dhParams);
        String encodedServerPublicKey = DiffieHellmanUtil.encodePublicKey(serverDHPublicKey);

        out.println("DH_PARAMS:" + encodedDHParams);
        out.println("DH_PUBLIC_KEY:" + encodedServerPublicKey);

        System.out.println("[" + clientAddress + "] DH parameters and public key sent");

        // Prano çelësin publik DH të klientit
        String clientDHMessage = in.readLine();
        if (clientDHMessage == null || !clientDHMessage.startsWith("CLIENT_DH_PUBLIC_KEY:")) {
            throw new Exception("Invalid client DH public key message");
        }

        String encodedClientPublicKey = clientDHMessage.substring("CLIENT_DH_PUBLIC_KEY:".length());
        PublicKey clientDHPublicKey = DiffieHellmanUtil.decodePublicKey(encodedClientPublicKey);

        System.out.println("[" + clientAddress + "] Received client's DH public key");

        // Llogarit sekretin e përbashkët
        aesKey = DiffieHellmanUtil.calculateAesKey(serverDHPrivateKey, clientDHPublicKey);

        System.out.println("[" + clientAddress + "] Shared secret established and AES key derived");
        out.println("ACK:DH_COMPLETE");
    }

    private void sendSignedWelcomeMessage() throws Exception {
        System.out.println("[" + clientAddress + "] Sending signed welcome message...");

        String welcomeMessage = "Welcome to Secure Server. Your connection is now encrypted with AES-128.";

        // Krijo nënshkrimin për mesazhin e mirëseardhjes
        String messageSignature = SignatureUtil.signMessage(welcomeMessage, serverPrivateKey);

        // Dërgo çelësin publik të serverit
        String encodedPublicKey = Base64.getEncoder().encodeToString(serverPublicKey.getEncoded());
        out.println("SERVER_PUBLIC_KEY:" + encodedPublicKey);

        // Dërgo mesazhin e mirëseardhjes të nënshkruar
        String encryptedWelcome = AESUtil.encrypt(welcomeMessage, aesKey);
        out.println("SIGNED_MESSAGE:" + encryptedWelcome + "|" + messageSignature);

        System.out.println("[" + clientAddress + "] Signed welcome message sent");

        // Prit konfirmimin e verifikimit
        String ackMessage = in.readLine();
        if (ackMessage != null && ackMessage.startsWith("SIGNATURE_VERIFIED")) {
            System.out.println("[" + clientAddress + "] Client verified signature successfully");
        }
    }

    private void handleEncryptedCommunication() throws Exception {
        System.out.println("[" + clientAddress + "] Ready for encrypted communication");

        String clientMessage;
        while ((clientMessage = in.readLine()) != null) {
            try {
                // Dekripto mesazhin e klientit
                String decryptedMessage = AESUtil.decrypt(clientMessage, aesKey);

                System.out.println("[" + clientAddress + "] Client: " + decryptedMessage);
                System.out.println("[" + clientAddress + "] Message Hash: " + messageHash);

                // Krijo përgjigjen e serverit
                String responseMessage = "Server received: " + decryptedMessage;

                // Nënshkruaj përgjigjen
                String responseSignature = SignatureUtil.signMessage(responseMessage, serverPrivateKey);

                // Kripto dhe dërgo përgjigjen me nënshkrim
                String encryptedResponse = AESUtil.encrypt(responseMessage, aesKey);
                out.println(encryptedResponse + "|" + responseSignature);

                System.out.println("[" + clientAddress + "] Response sent with signature");

            } catch (Exception e) {
                System.err.println("[" + clientAddress + "] Error processing message: " + e.getMessage());
                String errorResponse = AESUtil.encrypt("Error processing your message", aesKey);
                out.println(errorResponse);
            }
        }
    }
}