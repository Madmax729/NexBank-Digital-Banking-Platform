-- ==========================================
-- Digital Banking System - Database Initialization
-- Creates separate schemas for each microservice
-- ==========================================

-- Auth Service Schema
CREATE SCHEMA IF NOT EXISTS auth_schema;

-- User Service Schema
CREATE SCHEMA IF NOT EXISTS user_schema;

-- Account Service Schema
CREATE SCHEMA IF NOT EXISTS account_schema;

-- Transaction Service Schema
CREATE SCHEMA IF NOT EXISTS transaction_schema;

-- Ledger Service Schema
CREATE SCHEMA IF NOT EXISTS ledger_schema;

-- UPI Service Schema
CREATE SCHEMA IF NOT EXISTS upi_schema;

-- Fraud Service Schema
CREATE SCHEMA IF NOT EXISTS fraud_schema;

-- Notification Service Schema
CREATE SCHEMA IF NOT EXISTS notification_schema;

-- Audit Service Schema
CREATE SCHEMA IF NOT EXISTS audit_schema;

-- Admin Monitoring Schema
CREATE SCHEMA IF NOT EXISTS admin_schema;

-- Grant all privileges to banking_admin on all schemas
DO $$
DECLARE
    schema_name TEXT;
BEGIN
    FOR schema_name IN
        SELECT unnest(ARRAY['auth_schema', 'user_schema', 'account_schema',
                           'transaction_schema', 'ledger_schema', 'upi_schema',
                           'fraud_schema', 'notification_schema', 'audit_schema',
                           'admin_schema'])
    LOOP
        EXECUTE format('GRANT ALL ON SCHEMA %I TO banking_admin', schema_name);
        EXECUTE format('GRANT ALL ON ALL TABLES IN SCHEMA %I TO banking_admin', schema_name);
        EXECUTE format('ALTER DEFAULT PRIVILEGES IN SCHEMA %I GRANT ALL ON TABLES TO banking_admin', schema_name);
    END LOOP;
END $$;
