# Key Management Service

An independent microservice built with **Spring Boot**, **Java 21**, **PostgreSQL**, **Flyway**, and **Docker** to handle cryptographic key pair generation and management.

---

## Features

- **RSA Keypair Generation**: Generates 2048-bit RSA key pairs.
- **Security-at-Rest**: The private key is encrypted before storing in the database using **AES-256-GCM** using a master key configured via environment variables.
- **Key Deactivation**: Deactivates a key pair (`PATCH /keys/{id}/deactivate`).
- **Flyway Migrations**: Automated database schema migrations.
- **Dockerized Environment**: Ready to spin up PostgreSQL and application services using Docker Compose.

---

## Architecture and Cryptographic Details

1. **Key Generation**: Calling the key generation endpoint generates a new 2048-bit RSA keypair.
2. **Master Key Protection**: A 256-bit symmetric key (`APP_SECURITY_MASTER_KEY`) is used as a master key.
3. **AES-GCM Encryption**: The generated RSA private key is encrypted using AES in GCM mode (`AES/GCM/NoPadding`) with a randomly generated 12-byte IV.
4. **Database Storage**: The database stores the UUID (`keyId`), the public key in Base64 PEM format, the encrypted private key (`IV:ciphertext`), and status flags. The public key is safely exposed via HTTP API responses.

---

## Configuration

The application expects a `.env` file in the root directory for local runs, or environment variables in containerized environments.

Create a `.env` file in the root directory:
```properties
APP_SECURITY_MASTER_KEY=your_base64_encoded_256_bit_aes_master_key
```

---

## Getting Started

### Prerequisites

- Java 21 JDK
- Maven 3.x or Maven Wrapper (`mvnw`)
- Docker & Docker Compose (optional)

### Running locally with Docker

1. Start the PostgreSQL database:
   ```bash
   docker compose up -d db
   ```
   *Note: The Postgres host port is mapped to `5433` to prevent conflicts with other services running on `5432`.*

2. Run the application:
   ```bash
   ./mvnw spring-boot:run
   ```

### Running the entire stack in Docker

To build and spin up both the database and application:
```bash
docker compose up --build
```
The application will be accessible at `http://localhost:8081`.

---

## API Documentation

### 1. Generate Key Pair
- **Endpoint**: `POST /keys`
- **Response** (`201 Created`):
  ```json
  {
    "keyId": "550e8400-e29b-41d4-a716-446655440000",
    "publicKey": "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA..."
  }
  ```

### 2. Deactivate Key Pair
- **Endpoint**: `PATCH /keys/{id}/deactivate`
- **Response** (`204 No Content`)
