-- Local development database setup for PostgreSQL
-- Run these statements as a superuser (e.g., postgres)

-- Create a dedicated database for the app (ignore error if it already exists)
CREATE DATABASE medassist_backend_local;

-- (Optional) Ensure postgres owns the database
ALTER DATABASE medassist_backend_local OWNER TO postgres;

-- Note: No schema grants are necessary when using the postgres superuser.
-- If you connect using a non-superuser later, you may need to grant privileges on the schema:
-- \c medassist_backend_local -- (psql only)
-- GRANT ALL ON SCHEMA public TO your_user;
