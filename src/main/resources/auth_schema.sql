-- ============================================================
-- Auth Schema for Cinema Application
-- Run this script once to create the auth tables
-- ============================================================

-- Users table
CREATE TABLE IF NOT EXISTS cinema.users (
    user_id      BIGSERIAL PRIMARY KEY,
    email        VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name    VARCHAR(255),
    role         VARCHAR(50)  NOT NULL DEFAULT 'USER',  -- USER | ADMIN
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Refresh tokens (stored server-side to allow revocation)
CREATE TABLE IF NOT EXISTS cinema.refresh_tokens (
    token_id    BIGSERIAL   PRIMARY KEY,
    user_id     BIGINT      NOT NULL REFERENCES cinema.users(user_id) ON DELETE CASCADE,
    token_hash  VARCHAR(255) NOT NULL UNIQUE,   -- SHA-256 of the actual token
    expires_at  TIMESTAMPTZ NOT NULL,
    revoked     BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Password reset tokens (one-time use)
CREATE TABLE IF NOT EXISTS cinema.password_reset_tokens (
    token_id   BIGSERIAL   PRIMARY KEY,
    user_id    BIGINT      NOT NULL REFERENCES cinema.users(user_id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL UNIQUE,   -- SHA-256 of the actual token
    expires_at TIMESTAMPTZ NOT NULL,
    used       BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id   ON cinema.refresh_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_reset_tokens_user_id     ON cinema.password_reset_tokens(user_id);
