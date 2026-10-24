# Notification System

A microservice for sending notifications (Email, SMS, Webhook) using **Kafka** for asynchronous processing.

## Architecture

```mermaid
graph LR
    Client[E-commerce Platform] -->|POST /notifications|> NotificationSystem[Notification System]
    NotificationSystem -->|Save to MySQL|> MySQL[(MySQL)]
    NotificationSystem -->|Publish to Kafka|> Kafka[(Kafka)]
    Kafka -->|Consume|> Processor[Processor]
    Processor -->|Route to channel topic|> EmailNotifications[Email Notifications]
    Processor -->|Route to channel topic|> SMSNotifications[SMS Notifications]
    Processor -->|Route to channel topic|> WebhookNotifications[Webhook Notifications]
    EmailNotifications -->|Consume|> EmailWorker[Email Worker]
    SMSNotifications -->|Consume|> SMSWorker[SMS Worker]
    WebhookNotifications -->|Consume|> WebhookWorker[Webhook Worker]
    EmailWorker -->|Send Email|> Email[Email]
    SMSWorker -->|Send SMS|> SMS[SMS]
    WebhookWorker -->|Send Webhook|> Webhook[Webhook]
    EmailWorker -->|Update MySQL|> MySQL
    SMSWorker -->|Update MySQL|> MySQL
    WebhookWorker -->|Update MySQL|> MySQL
```

## How It Works

1. **Client sends request** - HTTP POST to `/notifications`
2. **Notification System saves to MySQL** - Status = `PENDING`
3. **Event published to Kafka** - Background workers will process it
4. **Processor routes to channel topic** - email/sms/webhook
5. **Workers consume from Kafka** - Read the event
6. **Send notification** - Email/SMS/etc.
7. **Update MySQL status** - Status = `SENT`

## Prerequisites

- **Java 17**
- **Maven**
- **Docker Desktop** (for MySQL + Kafka)

## Quick Start

### 1. Start Infrastructure

```bash
cd Notification-System
docker-compose up -d
```

This starts:
- **MySQL** on port `3307`
- **Kafka** on port `9092`

### 2. Run the Application

```bash
cd notification-service
mvn spring-boot:run
```

The application will start on `http://localhost:8082`

### 3. Send a Notification

```bash
curl -X POST http://localhost:8082/notifications \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 100,
    "channel": "EMAIL",
    "message": "Your order has been confirmed!"
  }'
```

Response:
```json
{
  "id": 1,
  "userId": 100,
  "channel": "EMAIL",
  "message": "Your order has been confirmed!",
  "status": "PENDING",
  "createdAt": "2024-10-24T18:00:00"
}
```

## API Endpoints

### Create Notification

**POST** `/notifications`

Request body:
```json
{
  "userId": 100,
  "channel": "EMAIL",
  "message": "Your order has been confirmed!",
  "scheduledAt": "2024-10-25T10:00:00"
}
```

**Channels:**
- `EMAIL`
- `SMS`
- `WEBHOOK`

**scheduledAt** (optional): If provided, notification will be sent at the specified time. If omitted or in the past, sent immediately.

**Response:** `202 Accepted` with notification details

### Get Notification Status

**GET** `/notifications/{id}`

Response:
```json
{
  "id": 1,
  "userId": 100,
  "channel": "EMAIL",
  "message": "Your order has been confirmed!",
  "status": "SENT",
  "createdAt": "2024-10-24T18:00:00",
  "sentAt": "2024-10-24T18:00:05"
}
```

**Statuses:**
- `PENDING` - Waiting to be sent
- `PROCESSING` - Being processed
- `SENT` - Successfully sent
- `FAILED` - Failed to send

## Kafka Topics

| Topic | Purpose |
|-------|---------|
| `notifications` | All notifications (entry point) |
| `email-notifications` | Email notifications only |
| `sms-notifications` | SMS notifications only |
| `webhook-notifications` | Webhook notifications only |

## Why Kafka?

- **Asynchronous** - API returns immediately, processing happens in background
- **Durable** - Messages are saved in Kafka, won't be lost
- **Scalable** - Multiple workers can process messages in parallel
- **Decoupled** - Producers and consumers don't know about each other

## Database Schema

**Table: `notifications`**

| Column | Type | Description |
|--------|------|-------------|
| `id` | BIGINT | Primary key |
| `user_id` | BIGINT | User ID |
| `channel` | VARCHAR | EMAIL/SMS/WEBHOOK |
| `message` | TEXT | Notification message |
| `status` | VARCHAR | PENDING/PROCESSING/SENT/FAILED |
| `scheduled_at` | TIMESTAMP | When to send (nullable) |
| `created_at` | TIMESTAMP | Creation time |
| `sent_at` | TIMESTAMP | When sent (nullable) |

## Configuration

### MySQL
- **Host:** `localhost:3307`
- **Database:** `notifications`
- **User:** `notification`
- **Password:** `notification`

### Kafka
- **Host:** `localhost:9092`
- **Mode:** KRaft (no ZooKeeper)

## Project Structure

```bash
notification-service/
├── controller/     ← HTTP API endpoints
├── service/        ← Business logic
├── processor/      ← Kafka router (notifications → channel topics)
├── worker/         ← Email/SMS/Webhook consumers
├── scheduler/      ← Timer for scheduled notifications
├── config/         ← Kafka topics configuration
├── domain/         ├── MySQL table mapping
├── repository/     ← SQL access
├── event/          ← Kafka message shape
├── dto/            ← HTTP JSON shape
└── model/          ← Enums (EMAIL/SMS/WEBHOOK, statuses)
```

## Integration with E-Commerce Platform

The E-Commerce Platform integrates with this system via HTTP:

```java
// In E-Commerce Platform
RestTemplate restTemplate = new RestTemplate();
restTemplate.postForObject(
    "http://localhost:8082/notifications",
    notificationRequest,
    NotificationResponse.class
);
```

For resilience, the E-Commerce Platform uses:
- **Retry** - 3 attempts with 1s delay
- **Circuit Breaker** - Opens after 50% failure rate
- **Async Processing** - Background thread for notifications

## Troubleshooting

**Notifications not being sent?**
- Check Docker is running (`docker ps`)
- Check MySQL and Kafka are accessible
- Check application logs for errors

**Kafka connection errors?**
- Verify Kafka is running on port 9092
- Check `docker-compose.yml` configuration
- Restart Kafka if needed

**Database connection errors?**
- Verify MySQL is running on port 3307
- Check database credentials in `application.yml`
- Access MySQL console: `mysql -h localhost -P 3307 -u notification -p`

## Technology Stack

- **Spring Boot 3.x**
- **Spring Kafka**
- **Spring Data JPA**
- **MySQL 8**
- **Apache Kafka 3.7.1** (KRaft mode)
- **Docker Compose**
