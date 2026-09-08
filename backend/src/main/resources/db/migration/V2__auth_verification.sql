-- Auth hardening + farmer verification workflow.
ALTER TABLE users
  ADD COLUMN account_status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  ADD COLUMN failed_login_attempts INTEGER NOT NULL DEFAULT 0,
  ADD COLUMN locked_until TIMESTAMP;

ALTER TABLE farmer_profiles
  ADD COLUMN onboarding_state VARCHAR(32) NOT NULL DEFAULT 'ACCOUNT_CREATED',
  ADD COLUMN fpo_validated BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE verification_records
  ADD COLUMN secret_hash VARCHAR(64);

CREATE TABLE refresh_tokens (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id),
  token_hash VARCHAR(64) UNIQUE NOT NULL,
  expires_at TIMESTAMP NOT NULL,
  revoked BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);
