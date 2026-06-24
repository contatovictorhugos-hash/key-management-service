# Key Management Service 🔑

A secure, independent microservice built with **Spring Boot** and **Java 21** designed to handle cryptographic key pair generation, secure database storage at rest, and hybrid payload decryption.

---

## 🏗️ Architecture Overview

The `key-management-service` serves as the centralized security hub. It generates asymmetric key pairs, safely stores private keys, and handles the decapsulation/decryption of secure payloads.

```
┌──────────────────────────┐                   ┌──────────────────────────┐
│   mailNotifierService    │                   │  key-management-service  │
│       (Port 8080)        │                   │       (Port 8081)        │
└────────────┬─────────────┘                   └────────────┬─────────────┘
             │                                              │
             │ 1. POST /keys (Generate keypair)             │
             ├─────────────────────────────────────────────►│
             │ 2. Returns: KeyId + PublicKey                │
             │◄─────────────────────────────────────────────┤
             │                                              │
             │ 3. Client encrypts email using PublicKey      │
             │    and sends payload via Brevo SMTP          │
             │                                              │
             │ 4. POST /keys/decrypt (Decrypt payload)      │
             ├─────────────────────────────────────────────►│
             │ 5. Returns: Decrypted Plaintext              │
             │◄─────────────────────────────────────────────┤
```

### Cryptographic Security Model

1. **RSA Keypair Generation**: Generates standard 2048-bit RSA keys.
2. **Encryption-at-Rest**: The RSA private key is encrypted before persisting to the PostgreSQL database using **AES-256-GCM** (`AES/GCM/NoPadding`) with a randomly generated 12-byte IV.
3. **Master Key**: The AES GCM key is derived from the `APP_SECURITY_MASTER_KEY` environment variable.
4. **Hybrid Payload Decryption**: Offers a safe way for companion services to decrypt content by sending the composite string `KeyId.EncriptKey.EncriptText`. The service recovers the private key, decrypts the session AES key (`RSA/ECB/PKCS1Padding`), and decrypts the final content (`AES/CBC/PKCS5Padding`).

---

## ✨ Features

- **RSA-2048 Key Generation**: Exposes public key for encryption.
- **AES-GCM Encryption**: Secures private keys in the database.
- **Deactivation Endpoint**: Safely deactivates key pairs (`PATCH /keys/{id}/deactivate`).
- **Decryption Endpoint**: Fully authenticates and decrypts hybrid payloads (`POST /keys/decrypt`).
- **Flyway Migrations**: Preconfigured PostgreSQL schema generation.
- **Docker Compose Setup**: Full Docker orchestration supporting application and PostgreSQL running in harmony.

---

## 🛠️ Configuration

Create a `.env` file in the root directory of the project:
```properties
APP_SECURITY_MASTER_KEY=your_base64_encoded_256_bit_aes_master_key
```

### Database Environment Variables (Docker Compose Default)
- **Database URL**: `jdbc:postgresql://localhost:5433/keymanagementdb`
- **Username**: `postgres`
- **Password**: `password`
- **Port**: `5433` *(Mapped to 5433 to prevent clashes with databases running on 5432)*

---

## 🚀 Getting Started

### Prerequisites
- Java 21 JDK
- Maven 3.x or Maven Wrapper (`mvnw`)
- Docker & Docker Compose

### Running Locally

1. **Spin up the database container**:
   ```bash
   docker compose up -d db
   ```
2. **Run the Spring Boot application**:
   ```bash
   ./mvnw spring-boot:run
   ```

### Running Entire Stack in Docker

Build and run both the PostgreSQL database and application in Docker containers:
```bash
docker compose up --build
```
The application will start and listen on port **`8081`**.

---

## 📖 API Documentation

### 1. Generate Key Pair
Generates a new RSA-2048 key pair, encrypts the private key, and stores it.
- **URL**: `/keys`
- **Method**: `POST`
- **Response** (`201 Created`):
  ```json
  {
    "keyId": "a1b2c3d4-e5f6-7a8b-9c0d-e1f2a3b4c5d6",
    "publicKey": "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA..."
  }
  ```

### 2. Deactivate Key Pair
Deactivates a key pair so it can no longer be used for decryption.
- **URL**: `/keys/{id}/deactivate`
- **Method**: `PATCH`
- **Response** (`204 No Content`)

### 3. Decrypt Hybrid Payload
Decrypts a composite payload matching the hybrid encryption format.
- **URL**: `/keys/decrypt`
- **Method**: `POST`
- **Content-Type**: `application/json`
- **Request Body**:
  ```json
  {
    "payload": "KeyId.EncriptKey.EncriptText"
  }
  ```
- **Response** (`200 OK`):
  ```json
  {
    "decryptedText": "My highly sensitive plaintext message"
  }
  ```
