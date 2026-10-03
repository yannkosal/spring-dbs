# Spring DBS

A small banking backend built as a set of Spring Boot microservices. It provides account management, transfers, payment-order creation, fraud checks, and event-driven notifications. Services communicate over REST and Apache Kafka; MySQL stores account, transaction, and payment data, while Redis supports rate limiting and fraud-related operations.

## Services

| Service | Port | Responsibility |
| --- | ---: | --- |
| API gateway | `8080` | Entry point for account, transaction, and payment API routes |
| Account service | `8081` | Creates and manages accounts and balances |
| Transaction service | `8082` | Initiates transfers, keeps transaction history, and coordinates transfer events |
| Payment service | `8083` | Creates Razorpay payment orders and processes payment webhooks |
| Fraud detection service | `8084` | Consumes transaction events and checks for suspicious activity |
| Notification service | `8085` | Consumes account, transaction, fraud, and payment events; currently logs notifications |

The gateway routes account requests under `/api/v1/accounts/**`, transaction requests under `/api/v1/transactions/**`, and payment requests under `/api/v1/payments/**`. Services also expose Actuator health endpoints at `/actuator/health`.

## Project environment

| Component | Version / configuration |
| --- | --- |
| JDK | Java 21 |
| Spring Boot | 4.1.1 |
| Spring Cloud | 2025.1.3 (gateway, transaction, and fraud-detection services) |
| Maven | 3.9.16 via the Maven Wrapper in each service |
| MySQL | 8.4 |
| Kafka / ZooKeeper | Confluent Platform 7.9.10 |
| Redis | `redis:latest` in Docker Compose |

The Spring Boot, Java, and Spring Cloud versions are defined in each service's `pom.xml`. Maven Wrapper downloads Maven when first run, so a separate Maven installation is not required.

## Prerequisites

- Git
- JDK 21 installed and available on `PATH` (`java -version`)
- Docker with the Docker Compose plugin (`docker compose version`)
- A terminal; commands below use macOS/Linux syntax

## Setup from scratch

1. Clone the repository and enter it:

   ```bash
   git clone https://github.com/yannkosal/spring-dbs.git
   cd spring-dbs
   ```

2. Start the local infrastructure:

   ```bash
   docker compose up -d mysql redis zookeeper kafka
   docker compose ps
   ```

   Wait for MySQL, Redis, and Kafka to finish starting before launching the Java services. To inspect startup logs, run `docker compose logs -f mysql kafka`.

3. Check the database and service settings before running the applications:

   - Compose initializes MySQL's `root` account with password `password`.
   - The account, transaction, and payment services connect to `localhost:3306` as `root` / `password`. Their databases (`account_db`, `transaction_db`, and `payment_db`) are created automatically.
   - Kafka is available at `localhost:9092`; Redis is at `localhost:6379`.
   - These are local development defaults. Change credentials and endpoints in the relevant `src/main/resources/application.yaml` files before using a non-local environment.
   - Payment order creation requires valid Razorpay credentials. Configure `RAZORPAY_KEY_SECRET` and `RAZORPAY_WEBHOOK_SECRET` in the environment before starting `payment-service`; update `razorpay.key-id` in its `application.yaml` to your Razorpay key ID. Without valid credentials, the payment integration will not be usable.

4. Build and test a service (repeat for each service as needed):

   ```bash
   cd account-service
   ./mvnw test
   ./mvnw package
   cd ..
   ```

5. Start each service in a separate terminal from the repository root:

   ```bash
   cd account-service && ./mvnw spring-boot:run
   ```

   Repeat in separate terminals for `transaction-service`, `fraud-detection-service`, `payment-service`, `notification-service`, and `api-gateway`:

   ```bash
   cd transaction-service && ./mvnw spring-boot:run
   cd fraud-detection-service && ./mvnw spring-boot:run
   cd payment-service && ./mvnw spring-boot:run
   cd notification-service && ./mvnw spring-boot:run
   cd api-gateway && ./mvnw spring-boot:run
   ```

   Each command should run in its own terminal because the service stays in the foreground. On Windows, use `mvnw.cmd` instead of `./mvnw`.

6. Check that a service has started, for example:

   ```bash
   curl http://localhost:8081/actuator/health
   ```

To stop the infrastructure containers, run `docker compose down` from the repository root. This keeps the MySQL data volume.

## API quick start

Create an account directly through the account service:

```bash
curl -X POST http://localhost:8081/api/v1/accounts \
  -H 'Content-Type: application/json' \
  -d '{
    "accountHolderName": "Alex Morgan",
    "email": "alex@example.com",
    "number": "5550100",
    "accountType": "SAVINGS",
    "initialDeposit": 1000
  }'
```

The account service returns the generated account details, including the account number. Use that number for account lookups and subsequent operations. The main API paths are:

| Operation | Method and path |
| --- | --- |
| Create account | `POST /api/v1/accounts` |
| Get account | `GET /api/v1/accounts/{accountNumber}` |
| Get balance | `GET /api/v1/accounts/{accountNumber}/balance` |
| Block account | `PUT /api/v1/accounts/{accountNumber}/block` |
| Initiate transfer | `POST /api/v1/transactions/transfer` |
| Get transaction | `GET /api/v1/transactions/{transactionId}` |
| Get account transaction history | `GET /api/v1/transactions/account/{accountNumber}` |
| Create payment order | `POST /api/v1/payments/create-order` |

For example, initiate a transfer through the transaction service with sender and receiver account numbers returned by the account API:

```bash
curl -X POST http://localhost:8082/api/v1/transactions/transfer \
  -H 'Content-Type: application/json' \
  -d '{
    "senderAccountNumber": "SENDER_ACCOUNT_NUMBER",
    "receiverAccountNumber": "RECEIVER_ACCOUNT_NUMBER",
    "amount": 25.00,
    "description": "Test transfer"
  }'
```

## Repository layout

```text
.
├── account-service/
├── api-gateway/
├── fraud-detection-service/
├── notification-service/
├── payment-service/
├── transaction-service/
└── docker-compose.yml
```

Each service is an independent Maven project with its own `pom.xml`, Maven Wrapper, application configuration, and Java source under `src/main/java`.
