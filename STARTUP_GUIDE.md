# 🏦 NexBank — Complete Startup Guide

## Prerequisites
- **Java 17+** (JDK)
- **Maven 3.8+**
- **Node.js 18+** (for frontend)
- **Docker & Docker Compose** (for infrastructure)

---

## 🔑 API Keys Setup (Optional)

| API | Purpose | How to Get | Required? |
|-----|---------|-----------|-----------|
| **ExchangeRate-API** | Live currency exchange rates | [exchangerate-api.com](https://www.exchangerate-api.com/) → "Get Free Key" | Optional (falls back to static rates) |

### Setup ExchangeRate-API:
1. Go to https://www.exchangerate-api.com/
2. Click **"Get Free Key"** → enter email → verify
3. Copy your API key from the dashboard
4. Paste into `.env`: `EXCHANGE_RATE_API_KEY=your_key_here`

> **Free tier:** 1,500 requests/month, 160+ currencies, no credit card required.

---

## ⚡ Quick Start (3 Steps)

### Step 1: Configure Environment
Copy `.env` and update values if needed:
```bash
# Required: Review and customize .env file
# Optional: Add your ExchangeRate-API key for live rates
# Optional: Change default passwords for production
```

### Step 2: Start Infrastructure
```bash
docker-compose up -d
```
This starts: PostgreSQL 16, Redis 7, Kafka (KRaft mode), Zipkin, **pgAdmin**

Wait ~15 seconds for all containers to be healthy:
```bash
docker-compose ps
```

### Step 3: Start Backend Services (Order Matters!)

Open **separate terminals** for each service:

```bash
# Terminal 1 — Discovery Server (MUST start first, wait 10s for it to be ready)
cd discovery-server
mvn spring-boot:run

# Terminal 2 — Config Server (start after Eureka is ready)
cd config-server
mvn spring-boot:run

# Terminal 3 — API Gateway (start after Config Server)
cd api-gateway
mvn spring-boot:run

# Terminal 4 — Auth Service
cd auth-service
mvn spring-boot:run

# Terminal 5 — User Service
cd user-service
mvn spring-boot:run

# Terminal 6 — Account Service
cd account-service
mvn spring-boot:run

# Terminal 7 — Transaction Service
cd transaction-service
mvn spring-boot:run

# Terminal 8 — Ledger Service
cd ledger-service
mvn spring-boot:run

# Terminal 9 — UPI Service
cd upi-service
mvn spring-boot:run

# Terminal 10 — Currency Service
cd currency-service
mvn spring-boot:run

# Terminal 11 — Fraud Service
cd fraud-service
mvn spring-boot:run

# Terminal 12 — Notification Service
cd notification-service
mvn spring-boot:run

# Terminal 13 — Audit Service
cd audit-service
mvn spring-boot:run

# Terminal 14 — Admin Monitoring Service
cd admin-monitoring-service
mvn spring-boot:run
```

### Step 4: Start Frontend
```bash
cd frontend
npm install
npm run dev
```
Open: **http://localhost:5173**

---

## 🔗 Service URLs

| Service              | URL                            |
|----------------------|--------------------------------|
| Frontend (React)     | http://localhost:5173           |
| API Gateway          | http://localhost:8080           |
| Eureka Dashboard     | http://localhost:8761           |
| **pgAdmin (DB UI)** | **http://localhost:5050**       |
| Zipkin (Tracing)     | http://localhost:9411           |
| Auth Service Swagger | http://localhost:8081/swagger-ui.html |
| Account Swagger      | http://localhost:8083/swagger-ui.html |
| Transaction Swagger  | http://localhost:8084/swagger-ui.html |

---

## 📊 Viewing Data in PostgreSQL

### Option 1: pgAdmin (Web UI) — Recommended
1. Open **http://localhost:5050**
2. Login: `admin@nexbank.com` / `admin123`
3. Click **"Add New Server"**:
   - **Name:** `NexBank`
   - **Connection → Host:** `postgres`
   - **Port:** `5432`
   - **Username:** `banking_admin`
   - **Password:** `BankingSecure@2024`
4. Expand: **NexBank → Databases → banking_system → Schemas**
5. Browse: `auth_schema`, `account_schema`, `transaction_schema`, etc.
6. Right-click any table → **View/Edit Data → All Rows**

### Option 2: psql CLI
```bash
docker exec -it banking-postgres psql -U banking_admin -d banking_system

# Useful commands:
\dn                              -- list all schemas
SET search_path TO auth_schema;  -- switch schema
\dt                              -- list tables
SELECT * FROM users;             -- query data
\q                               -- quit
```

---

## ⚙️ Environment Variables (.env)

All configuration is externalized in the `.env` file:

| Variable | Purpose | Default |
|----------|---------|---------|
| `POSTGRES_PASSWORD` | Database password | `BankingSecure@2024` |
| `REDIS_PASSWORD` | Cache password | `RedisSecure@2024` |
| `JWT_SECRET` | Token signing key | Base64-encoded secret |
| `EXCHANGE_RATE_API_KEY` | Live currency rates | Empty (uses static rates) |
| `FRAUD_LARGE_AMOUNT_THRESHOLD` | Flag transactions above this amount | `100000` |
| `FRAUD_CROSS_CURRENCY_THRESHOLD` | Flag cross-currency transfers above this | `50000` |
| `CORS_ALLOWED_ORIGINS` | Allowed frontend URLs | `http://localhost:3000,http://localhost:5173` |
| `PGADMIN_EMAIL` | pgAdmin login email | `admin@nexbank.com` |
| `PGADMIN_PASSWORD` | pgAdmin login password | `admin123` |

---

## 🌓 Light/Dark Mode

The frontend supports light and dark mode:
- Click the **☀️/🌙 toggle** in the top-right header bar
- Theme preference is saved in localStorage
- Respects your OS system preference by default

---

## 🛑 Stopping Everything

```bash
# Stop frontend: Ctrl+C in the terminal

# Stop all backend services: Ctrl+C in each terminal

# Stop infrastructure:
docker-compose down

# To also remove data volumes:
docker-compose down -v
```

---

## 🧪 Testing the API (without frontend)

### Register a user:
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"fullName":"John Doe","email":"john@test.com","password":"Test@1234"}'
```

### Login:
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"john@test.com","password":"Test@1234"}'
```

### Create Account (use token from login):
```bash
curl -X POST http://localhost:8080/api/user/accounts \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"accountHolderName":"John Doe","accountType":"SAVINGS","currency":"INR"}'
```

### Check Live Exchange Rates:
```bash
curl http://localhost:8080/api/currency/rates
```
