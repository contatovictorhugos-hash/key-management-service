# Novo Microserviço: key-service

Este plano detalha a estrutura de arquivos, dependências e código para um novo microserviço independente (`key-service`) construído com Spring Boot 3.3.x, Java 21, JPA, PostgreSQL e Flyway.

O serviço gerará um identificador exclusivo (`KeyId`) e um par de chaves RSA, armazenando-os no PostgreSQL (com a chave privada criptografada por segurança) e disponibilizando a chave pública através de uma requisição `GET`.

## Arquitetura de Segurança

> [!IMPORTANT]
> **Criptografia da Chave Privada em Repouso**: Por motivos de segurança, a chave privada RSA gerada não deve ser armazenada em texto plano no banco de dados. Utilizaremos o algoritmo AES-256 para criptografar a chave privada antes de salvá-la no banco. A chave simétrica do AES será obtida de uma propriedade do Spring (`app.security.master-key`) configurada por variável de ambiente.

## Estrutura do Novo Projeto

O projeto terá a seguinte estrutura de arquivos típica do Spring Boot Maven:
```text
key-service/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── keyservice/
│   │   │           ├── KeyServiceApplication.java
│   │   │           ├── controller/
│   │   │           │   └── KeyController.java
│   │   │           ├── dto/
│   │   │           │   └── KeyResponseDTO.java
│   │   │           ├── exception/
│   │   │           │   └── KeyNotFoundException.java
│   │   │           ├── model/
│   │   │           │   └── CryptoKey.java
│   │   │           ├── repository/
│   │   │           │   └── CryptoKeyRepository.java
│   │   │           ├── service/
│   │   │           │   └── KeyService.java
│   │   │           └── util/
│   │   │               └── CryptoUtils.java
│   │   └── resources/
│   │       ├── application.yml
│   │       └── db/
│   │           └── migration/
│   │               └── V1__create_keys_table.sql
```

## Proposed Changes (Arquivos a serem criados)

Abaixo estão definidos os conteúdos para cada arquivo a ser criado no novo projeto.

---

### 1. Configurações do Projeto e Banco de Dados

#### [NEW] `pom.xml`
Configuração do Maven com Spring Boot 3.3.0, Java 21, JPA, Postgres, Flyway, Lombok e DevTools.

#### [NEW] `src/main/resources/application.yml`
Propriedades de conexão ao PostgreSQL, Flyway e a chave mestra para criptografia da chave privada.

#### [NEW] `src/main/resources/db/migration/V1__create_keys_table.sql`
Script Flyway para criação da tabela de chaves.

---

### 2. Classes de Domínio e Infraestrutura

#### [NEW] `src/main/java/com/keyservice/model/CryptoKey.java`
Entidade JPA para mapear a tabela `tb_keys`.

#### [NEW] `src/main/java/com/keyservice/repository/CryptoKeyRepository.java`
Repositório Spring Data JPA para as chaves.

#### [NEW] `src/main/java/com/keyservice/dto/KeyResponseDTO.java`
DTO para retorno de chaves (retorna apenas KeyId e Chave Pública).

---

### 3. Utilitários e Lógica de Negócio

#### [NEW] `src/main/java/com/keyservice/util/CryptoUtils.java`
Componente contendo funções utilitárias:
- Geração do par de chaves RSA (2048 bits).
- Conversão de chaves para o formato PEM.
- Criptografia e descriptografia AES-256 para proteger a chave privada.

#### [NEW] `src/main/java/com/keyservice/service/KeyService.java`
Serviço com a lógica de:
- Gerar chaves, criptografar a chave privada e persistir no banco de dados.
- Buscar chave pública por ID.

---

### 4. Interface REST / Controllers

#### [NEW] `src/main/java/com/keyservice/controller/KeyController.java`
Exposição dos endpoints `POST /keys` e `GET /keys/{id}`.

#### [NEW] `src/main/java/com/keyservice/KeyServiceApplication.java`
Classe de inicialização do Spring Boot.

---

## Verification Plan

### Manual Verification
1. Subir um banco PostgreSQL local (ex: via Docker).
2. Executar o microserviço `key-service`.
3. Chamar `POST http://localhost:8080/keys` para criar a chave.
4. Validar se a chave foi criada no banco de dados e se a chave privada está criptografada.
5. Chamar `GET http://localhost:8080/keys/{keyId}` e validar se retorna a chave pública PEM correta.
