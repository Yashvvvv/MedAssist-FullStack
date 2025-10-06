-- MedAssist Authentication Database Setup Script
-- Execute this script to create the dedicated authentication database

-- Create database and user (PostgreSQL syntax)
CREATE DATABASE medassist_auth;
CREATE USER medassist_user WITH PASSWORD 'medassist_password';
GRANT ALL PRIVILEGES ON DATABASE medassist_auth TO medassist_user;

-- Connect to the database
\c medassist_auth;

-- Grant schema privileges
GRANT ALL ON SCHEMA public TO medassist_user;

-- Note: Tables will be automatically created by Hibernate based on the entity classes
-- The following is just for reference of what will be created:

/*
Tables that will be auto-created:
- users (main user table with authentication data)
- roles (role definitions)
- permissions (permission definitions)
- user_roles (many-to-many relationship between users and roles)
- role_permissions (many-to-many relationship between roles and permissions)
- verification_tokens (email verification tokens)
- password_reset_tokens (password reset tokens)
*/

-- Optional: Create indexes for better performance (execute after tables are created)
-- CREATE INDEX idx_users_email ON users(email);
-- CREATE INDEX idx_users_username ON users(username);
-- CREATE INDEX idx_verification_tokens_token ON verification_tokens(token);
-- CREATE INDEX idx_password_reset_tokens_token ON password_reset_tokens(token);
