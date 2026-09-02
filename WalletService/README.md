# Wallet Service (Port 8083)

## Overview
The **Wallet Service** maintains financial ledgers, rider balances, driver payout balances, Razorpay payment gateway checkout orders, and automated payment deduction upon ride completion.

## Database
- **Engine**: PostgreSQL
- **Database**: `ridemesh_wallet_db`

## Kafka Topics
- **Consumes**:
  - `ride-ended-event`: Deducts ride fare from rider wallet and credits driver wallet.
- **Produces**:
  - `payment-completed-event`: Notifies `NotifyService` to send a payment receipt email.

## API Endpoints & Swagger Documentation
- **Swagger UI**: [http://localhost:8083/swagger-ui/index.html](http://localhost:8083/swagger-ui/index.html)

### Wallet Operations
- `GET /wallet/balance` - Retrieve current user wallet balance
- `POST /wallet/create-order` - Create a Razorpay top-up order
- `POST /webhooks/razorpay` - Webhook callback for verified deposit confirmation
