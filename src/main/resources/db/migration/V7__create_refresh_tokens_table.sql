-- V7: Create refresh_tokens table for JWT refresh token management
CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    revoked_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Index for token lookup (hash-based)
CREATE INDEX idx_refresh_tokens_hash ON refresh_tokens(token_hash);

-- Index for user's tokens (for revocation/listing)
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);

-- Index for cleanup of expired tokens
CREATE INDEX idx_refresh_tokens_expires ON refresh_tokens(expires_at) WHERE revoked = FALSE;

-- Partial index for active tokens only
CREATE INDEX idx_refresh_tokens_active ON refresh_tokens(token_hash) WHERE revoked = FALSE;
