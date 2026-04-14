# Circulation Service - RabbitMQ Event Architecture

## Overview

The Circulation Service publishes events to RabbitMQ for asynchronous communication with other services (notification-service, system-service).

## Event Types

### 1. OVERDUE_DETECTED Event
**When**: Triggered when a borrow record is detected as overdue during the daily scheduled task.

**Exchange**: `notification.topic`
**Routing Key**: `notification.email.overdue`
**Target Queue**: `notification.email.queue`

**Payload**:
```json
{
  "eventId": "EVT-1732956000000-abc12345",
  "eventType": "OVERDUE_DETECTED",
  "eventTime": "2025-11-30T10:00:00",
  "borrowId": 123,
  "borrowIdStr": "BW-20251120-0001",
  "readerId": 456,
  "readerName": "Zhang San",
  "readerEmail": "zhangsan@example.com",
  "readerPhone": "138****1234",
  "bookId": 789,
  "bookTitle": "Java Programming",
  "bookBarcode": "BC-789",
  "dueDate": "2025-11-25T23:59:59",
  "overdueDays": 2,
  "currentFineAmount": 0.20,
  "notificationType": "EMAIL"
}
```

### 2. RETURN_COMPLETED Event
**When**: Triggered when a book is successfully returned.

**Exchange**: `system.log.topic`
**Routing Key**: `system.log.circulation.return`
**Target Queue**: `system.log.queue`

**Payload**:
```json
{
  "eventId": "EVT-1732956000000-def67890",
  "eventType": "RETURN_COMPLETED",
  "eventTime": "2025-11-30T14:30:00",
  "borrowId": 123,
  "borrowIdStr": "BW-20251120-0001",
  "readerId": 456,
  "readerName": "Zhang San",
  "bookId": 789,
  "bookTitle": "Java Programming",
  "bookBarcode": "BC-789",
  "borrowDate": "2025-11-20T10:00:00",
  "dueDate": "2025-12-20T23:59:59",
  "returnDate": "2025-11-30T14:30:00",
  "isOverdue": false,
  "overdueDays": 0,
  "fineAmount": 0.00,
  "finePaid": true,
  "operatorId": null,
  "operatorName": null
}
```

### 3. RESERVATION_READY Event
**When**: Triggered when a reserved book becomes available for pickup.

**Exchange**: `notification.topic`
**Routing Key**: `notification.sms.reservation`
**Target Queue**: `notification.sms.queue`

**Payload**:
```json
{
  "eventId": "EVT-1732956000000-ghi11111",
  "eventType": "RESERVATION_READY",
  "eventTime": "2025-11-30T09:00:00",
  "reserveId": 321,
  "reserveIdStr": "RV-20251125-0001",
  "readerId": 456,
  "readerName": "Zhang San",
  "readerPhone": "13812345678",
  "readerEmail": "zhangsan@example.com",
  "bookId": 789,
  "bookTitle": "Java Programming",
  "bookIsbn": "978-7-111-12345-6",
  "reserveDate": "2025-11-25T10:00:00",
  "expiryDate": "2025-12-02T23:59:59",
  "pickupLocation": "Main Library Counter",
  "notificationType": "SMS",
  "remainingDays": 7
}
```

### 4. FINE_PAID Event
**When**: Triggered when a fine is successfully paid.

**Exchange**: `system.log.topic`
**Routing Key**: `system.log.circulation.fine`
**Target Queue**: `system.log.queue`

**Payload**:
```json
{
  "eventId": "EVT-1732956000000-jkl22222",
  "eventType": "FINE_PAID",
  "eventTime": "2025-11-30T15:00:00",
  "borrowId": 123,
  "borrowIdStr": "BW-20251120-0001",
  "readerId": 456,
  "readerName": "Zhang San",
  "bookId": 789,
  "bookTitle": "Java Programming",
  "fineAmount": 5.00,
  "paymentMethod": "CASH",
  "paidTime": "2025-11-30T15:00:00",
  "remarks": "Paid at counter"
}
```

## Fine Calculation Rules

| Rule | Value |
|------|-------|
| Grace Period | 3 days |
| Fine Per Day | 0.10 CNY |
| Maximum Fine | 50.00 CNY |

**Formula**:
```
actualOverdueDays = max(0, totalOverdueDays - 3)
calculatedFine = actualOverdueDays * 0.10
finalFine = min(calculatedFine, 50.00)
```

## RabbitMQ Configuration

### Exchanges
| Exchange Name | Type | Durable |
|---------------|------|---------|
| `circulation.topic` | Topic | Yes |
| `notification.topic` | Topic | Yes |
| `system.log.topic` | Topic | Yes |

### Queues
| Queue Name | TTL | Dead Letter Exchange |
|------------|-----|---------------------|
| `notification.email.queue` | 10 min | notification.dlx |
| `notification.sms.queue` | 10 min | notification.dlx |
| `system.log.queue` | 1 hour | - |

### Connection Settings
```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: admin
    password: admin_secure_2024
    virtual-host: /
    publisher-confirm-type: correlated
    publisher-returns: true
```

## Scheduled Tasks

| Task | Schedule | Description |
|------|----------|-------------|
| Update Overdue Status | Daily 6:00 AM | Marks BORROWED records as OVERDUE if past due date |
| Expire Reservations | Hourly | Marks RESERVED records as EXPIRED if past expiry date |
| Due Soon Reminders | Daily 10:00 AM | Sends reminders for books due within 1 day |

## Error Handling

- Event publishing failures are logged but do not affect the main business flow
- Events include unique eventId for tracking and deduplication
- Publisher confirms are enabled for reliable delivery
- Return callbacks log any routing failures

## Integration Points

### Notification Service (Consumer)
- Listens on `notification.email.queue` for email notifications
- Listens on `notification.sms.queue` for SMS notifications

### System Service (Consumer)
- Listens on `system.log.queue` for audit logging
