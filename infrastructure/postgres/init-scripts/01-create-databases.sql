-- Create separate databases for each microservice
-- This script runs automatically when PostgreSQL container starts

-- Patient Service Database
CREATE DATABASE patient_db;

-- Laboratory Service Database
CREATE DATABASE lab_db;

-- Insurance Service Database
CREATE DATABASE insurance_db;

-- Pharmacy Service Database (for future use)
CREATE DATABASE pharmacy_db;

-- Audit Service Database (for future use)
CREATE DATABASE audit_db;

-- Keycloak Database
CREATE DATABASE keycloak;

-- Grant privileges to meditrack user
GRANT ALL PRIVILEGES ON DATABASE patient_db TO meditrack;
GRANT ALL PRIVILEGES ON DATABASE lab_db TO meditrack;
GRANT ALL PRIVILEGES ON DATABASE insurance_db TO meditrack;
GRANT ALL PRIVILEGES ON DATABASE pharmacy_db TO meditrack;
GRANT ALL PRIVILEGES ON DATABASE audit_db TO meditrack;
GRANT ALL PRIVILEGES ON DATABASE keycloak TO meditrack;

-- Log database creation
\echo 'Created all MediTrack service databases successfully'
