# Diffie-Hellman-and-Digital-Signatures-Console-Application
A secure client-server console application that uses the Diffie–Hellman key exchange algorithm to establish a shared secret key and digital signatures to verify message integrity and authenticity during communication.
# Secure Chat DH Signatures

## Description

A secure chat application built in Java using:

- Socket Programming
- Diffie-Hellman Key Exchange
- AES Encryption
- RSA Digital Signatures
- SHA-256 Hashings

---

# Team Members

## Person 1 — Server + Socket Communication

Files:

- Server.java
- ClientHandler.java

---

## Person 2 — Diffie-Hellman Key Exchange

Files:

- DiffieHellmanUtil.java
- KeyExchangeService.java

---

## Person 3 — Digital Signatures + Hashing

Files:

- SignatureUtil.java
- HashUtil.java

---

## Person 4 — AES Encryption + Console UI + README

Files:

- AESUtil.java
- Client.java
- README.md

---

# Project Structure

```text
secure-chat-dh-signatures/

├── server/
├── client/
├── crypto/
├── README.md
└── pom.xml
```

---

# How To Run

Compile:

```bash
mvn clean compile
```

Run Server:

```bash
java server.Server
```

Run Client:

```bash
java client.Client
```

---

# Git Workflow

Each team member works in a separate branch and creates a Pull Request before merging into main.