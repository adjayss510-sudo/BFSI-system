-- Runs automatically on first start of the Postgres container (docker-compose).
-- One database per microservice (database-per-service pattern).
CREATE DATABASE auth_db;
CREATE DATABASE customer_db;
CREATE DATABASE payment_db;
CREATE DATABASE ledger_db;
CREATE DATABASE notification_db;
