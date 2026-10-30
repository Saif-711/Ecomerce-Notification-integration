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
- **Resend API Key** (for real email delivery) - Get one at https://resend.com

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

### 3. Configure Email Provider (Resend)

Option 1: Using `.env` file (recommended for development)

Create a `.env` file in the `Notification-System` directory:

```bash
RESEND_API_KEY=your-resend-api-key-here
RESEND_FROM_EMAIL=noreply@yourdomain.com
```

The application uses `spring-dotenv` to automatically load environment variables from the `.env` file.

Option 2: Using environment variables

```bash
# Windows PowerShell
$env:RESEND_API_KEY="your-resend-api-key"
$env:RESEND_FROM_EMAIL="your-email@yourdomain.com"

# Linux/Mac
export RESEND_API_KEY="your-resend-api-key"
export RESEND_FROM_EMAIL="your-email@yourdomain.com"
```

**Note:** Get your API key from https://resend.com. The `from` email must be a verified domain in your Resend account.

### 4. Send a Notification

```bash
curl -X POST http://localhost:8082/notifications \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 100,
    "channel": "EMAIL",
    "message": "Your order has been confirmed!",
    "recipientEmail": "customer@example.com"
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

**Note:** `recipientEmail` is optional. If not provided, the system uses a fallback email format.

## API Endpoints

### Create Notification

**POST** `/notifications`

Request body:
```json
{
  "userId": 100,
  "channel": "EMAIL",
  "message": "Your order has been confirmed!",
  "recipientEmail": "customer@example.com",
  "scheduledAt": "2024-10-25T10:00:00"
}
```

**Channels:**
- `EMAIL`
- `SMS`
- `WEBHOOK`

**recipientEmail** (optional): For EMAIL channel, specify the recipient email. If not provided, a fallback email format is used.

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
| `id` | VARCHAR | Primary key (UUID) |
| `user_id` | BIGINT | User ID |
| `channel` | VARCHAR | EMAIL/SMS/WEBHOOK |
| `message` | TEXT | Notification message |
| `recipient_email` | VARCHAR | Recipient email for EMAIL channel (nullable) |
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

### Resend (Email Provider)
- **API Key:** Set via `RESEND_API_KEY` environment variable
- **From Email:** Set via `RESEND_FROM_EMAIL` environment variable
- **Get API Key:** https://resend.com

## Email Integration with Resend

The Notification System now integrates with **Resend** for real email delivery.

### How It Works

1. **E-Commerce Platform** sends notification request with `recipientEmail`
2. **Notification System** saves to MySQL with recipient email
3. **Kafka** routes to email topic
4. **EmailWorker** uses Resend API to send real email
5. **MySQL** status updated to `SENT` on success

### Configuration

Option 1: Using `.env` file (recommended)

Create a `.env` file in the Notification-System directory:

```bash
RESEND_API_KEY=re_xxxxxxxxxxxx
RESEND_FROM_EMAIL=noreply@yourdomain.com
```

Option 2: Using environment variables

```bash
export RESEND_API_KEY="re_xxxxxxxxxxxx"
export RESEND_FROM_EMAIL="noreply@yourdomain.com"
```

### Benefits

- **Real Email Delivery** - Actual emails sent to recipients' inboxes
- **Reliable** - Resend handles deliverability, retries, and bounces
- **Scalable** - No need to manage SMTP servers
- **Trackable** - Get email delivery status via Resend dashboard

### Resend Free Tier Limitations

**Important:** This project uses Resend's **free tier** for development/testing, which has the following limitation:

- **Only verified recipient emails** can receive emails when using the free `onboarding@resend.dev` domain
- You must verify recipient emails in your Resend dashboard before sending to them
- This is a Resend restriction, not a bug in the notification system

**For production use:**
1. Upgrade to a paid Resend plan with your own domain (e.g., `noreply@yourdomain.com`)
2. Configure DNS records (SPF, DKIM) for your domain
3. Once using your own domain, you can send to any email address without verification

**Current setup:**
- Using free tier with `onboarding@resend.dev`
- Only verified emails (like the account owner's email) will receive notifications
- Unverified recipient emails will fail with status `FAILED`

### Testing Without API Key

If you don't have a Resend API key, the system will log an error but won't crash. The email won't be sent, and the notification status won't be updated to `SENT`.

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
- **Resend** (Email delivery)
