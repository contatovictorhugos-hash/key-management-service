# Key Management Service

> Generates, stores, and manages AES cryptographic keys used to encrypt sensitive payloads across companion services.

![Java](https://img.shields.io/badge/Java-21-ED8B00?style=flat-square&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.x-6DB33F?style=flat-square&logo=springboot&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-C71A36?style=flat-square&logo=apachemaven&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=flat-square&logo=docker&logoColor=white)

## Table of Contents
- [Overview](#overview)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Getting Started](#getting-started)
- [Project Structure](#project-structure)
- [API Reference](#api-reference)
- [Ecosystem](#ecosystem)
- [Contributing](#contributing)

## Overview

Key Management Service handles the full lifecycle of AES cryptographic keys — generation, storage, deactivation, and payload decryption. It exposes a REST API that companion services (such as [Mail Notifier Service](../mail-notifier-service)) call to obtain encryption keys before securing sensitive data. Keys are persisted in a PostgreSQL database with schema versioning managed by Flyway. A configurable master key protects the key generation process.

## Architecture

The application follows a standard Spring Boot layered architecture:

```
Controller (KeyController)
    ↓
Service (KeyService, CryptoService)
    ↓
Repository (CryptoKeyRepository)
    ↓
PostgreSQL (keymanagementdb)
```

- **KeyController** — Receives HTTP requests for key generation, deactivation, and decryption.
- **KeyService** — Orchestrates key lifecycle operations and delegates cryptographic work.
- **CryptoService** — Performs AES encryption and decryption using a master key.
- **CryptoKeyRepository** — Spring Data JPA repository for the `CryptoKey` entity.
- **GlobalExceptionHandler** — Centralized error handling with structured `ApiError` responses.

### Containerisation

A multi-stage `Dockerfile` builds the application with Maven and produces a lightweight JRE image. The `docker-compose.yml` provisions both the application container (port `8081`) and a PostgreSQL 15 Alpine container (port `5433`).

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.x |
| ORM | Spring Data JPA (Hibernate) |
| Database | PostgreSQL 15 |
| Migrations | Flyway |
| Validation | Jakarta Bean Validation |
| Build | Maven (with Maven Wrapper) |
| Containerisation | Docker, Docker Compose |

## Getting Started

### Prerequisites

- Java 21+
- Maven 3.9+ (or use the included `mvnw` wrapper)
- Docker and Docker Compose (for containerised setup)

### Installation

```bash
# Clone the repository
git clone https://github.com/contatovictorhugos/key-management-service.git
cd key-management-service

# Install dependencies
./mvnw clean install
```

### Configuration

| Variable | Description | Example |
|---|---|---|
| `APP_SECURITY_MASTER_KEY` | Master key used for AES key generation | *(secret — set in `.env` or environment)* |
| `SPRING_DATASOURCE_URL` | JDBC connection string | `jdbc:postgresql://localhost:5433/keymanagementdb` |
| `SPRING_DATASOURCE_USERNAME` | Database username | `postgres` |
| `SPRING_DATASOURCE_PASSWORD` | Database password | `password` |

### Running locally

```bash
# Start the database with Docker Compose
docker compose up -d db

# Run the application
./mvnw spring-boot:run
```

Or run the full stack in containers:

```bash
docker compose up --build
```

The service starts on **port 8081** (mapped from container port 8080).

## Project Structure

```
key-management-service/
├── src/
│   ├── main/
│   │   ├── java/com/key_management_service/
│   │   │   ├── controller/      # REST endpoints
│   │   │   ├── dto/             # Request/response DTOs
│   │   │   ├── exception/       # Custom exceptions and global handler
│   │   │   ├── model/           # JPA entities
│   │   │   ├── repository/      # Spring Data repositories
│   │   │   └── service/         # Business logic and cryptography
│   │   └── resources/
│   │       ├── db/migration/    # Flyway SQL migrations
│   │       └── application.properties
│   └── test/                    # Unit and integration tests
├── Dockerfile
├── docker-compose.yml
└── pom.xml
```

## API Reference

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/keys` | Generates a new AES key pair and returns the public key material |
| `PATCH` | `/keys/{id}/deactivate` | Deactivates a key by UUID |
| `POST` | `/keys/decrypt` | Decrypts an encrypted payload using the stored key |

## Ecosystem

This project is part of the **Projetcs** suite. The following projects work together:

| Project | Role | Depends On |
|---|---|---|
| **key-management-service** | REST API — cryptographic key lifecycle management | PostgreSQL |
| **mail-notifier-service** | REST API — transactional email delivery with encryption | key-management-service API, PostgreSQL, Brevo |
| **fipe-csv** | REST API — FIPE vehicle pricing table to CSV export | FIPE public API |
| **bko-project** | Server-rendered web app — internal backoffice administration | PostgreSQL |
| **split-csv** | CLI tool — splits large CSV files into smaller parts | — |
| **mergeCSV** | CLI tool — merges multiple CSV files into one | — |
| **prj_extensao** | Mobile app (React Native / Expo) — Methodist church community app | Firebase |

> **This project**: `key-management-service` provides the cryptographic key API consumed by `mail-notifier-service` to encrypt email bodies before delivery.

## Contributing

1. Fork the repository.
2. Create a feature branch: `git checkout -b feature/your-feature-name`
3. Commit your changes: `git commit -m 'feat: add your feature'`
4. Push to the branch: `git push origin feature/your-feature-name`
5. Open a Pull Request.

Please follow [Conventional Commits](https://www.conventionalcommits.org/) for commit messages.
