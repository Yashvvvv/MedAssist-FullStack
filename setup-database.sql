-- MedAssist Database Setup Script
-- Run this in PostgreSQL to set up the database

-- Create database
CREATE DATABASE medassist_auth;

-- Create user
CREATE USER medassist_user WITH PASSWORD 'medassist_password';

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE medassist_auth TO medassist_user;

-- Connect to the database (you may need to run this separately)
\c medassist_auth;

-- Grant schema privileges
GRANT ALL ON SCHEMA public TO medassist_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO medassist_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO medassist_user;

-- Enable necessary extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Basic test
SELECT 'Database setup completed successfully!' as status;
