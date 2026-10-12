-- V1: Initial schema for SecureChat
-- Uses MySQL 8 syntax (ENUM, MEDIUMTEXT, etc.)
-- H2 compatibility: Flyway baseline-on-migrate handles H2 in tests.

-- ============================================================
-- USERS
-- ============================================================
CREATE TABLE IF NOT EXISTS users (
    id             BIGINT          NOT NULL AUTO_INCREMENT,
    name           VARCHAR(80)     NOT NULL,
    email          VARCHAR(255)    NOT NULL,
    password_hash  VARCHAR(255)    NOT NULL,
    last_seen      DATETIME(3),
    created_at     DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    totp_secret    VARCHAR(64),
    totp_enabled   BOOLEAN         NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email)
);

-- ============================================================
-- USER KEYS (E2E public key storage)
-- ============================================================
CREATE TABLE IF NOT EXISTS user_keys (
    user_id                        BIGINT          NOT NULL,
    public_key                     TEXT            NOT NULL,
    key_fingerprint                VARCHAR(80)     NOT NULL,
    encrypted_private_key_backup   TEXT,
    kdf_salt                       VARCHAR(32),
    kdf_iterations                 INT,
    created_at                     DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (user_id),
    CONSTRAINT fk_user_keys_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- ============================================================
-- CONVERSATIONS
-- ============================================================
CREATE TABLE IF NOT EXISTS conversations (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    type        VARCHAR(10)     NOT NULL,   -- 'DIRECT' or 'GROUP'
    title       VARCHAR(200),
    direct_key  VARCHAR(64),               -- "minId:maxId" for direct chats (unique constraint ensures one per pair)
    created_by  BIGINT          NOT NULL,
    created_at  DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_conversations_direct_key (direct_key),
    CONSTRAINT fk_conv_creator FOREIGN KEY (created_by) REFERENCES users(id)
);

-- ============================================================
-- CONVERSATION MEMBERS
-- ============================================================
CREATE TABLE IF NOT EXISTS conversation_members (
    conversation_id     BIGINT          NOT NULL,
    user_id             BIGINT          NOT NULL,
    role                VARCHAR(10)     NOT NULL DEFAULT 'MEMBER',  -- 'ADMIN' or 'MEMBER'
    joined_at           DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    last_read_message_id BIGINT,
    PRIMARY KEY (conversation_id, user_id),
    CONSTRAINT fk_cm_conversation FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE,
    CONSTRAINT fk_cm_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_cm_user_id ON conversation_members(user_id);

-- ============================================================
-- MESSAGES
-- ============================================================
CREATE TABLE IF NOT EXISTS messages (
    id                BIGINT          NOT NULL AUTO_INCREMENT,
    conversation_id   BIGINT          NOT NULL,
    sender_id         BIGINT          NOT NULL,
    client_message_id VARCHAR(36)     NOT NULL,
    type              VARCHAR(10)     NOT NULL DEFAULT 'TEXT',   -- 'TEXT','IMAGE','FILE'
    ciphertext        MEDIUMTEXT      NOT NULL,
    iv                VARCHAR(32)     NOT NULL,
    key_version       INT             NOT NULL DEFAULT 1,
    reply_to_id       BIGINT,
    created_at        DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    edited_at         DATETIME(3),
    deleted_at        DATETIME(3),
    expires_at        DATETIME(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_messages_client_id (sender_id, client_message_id),
    CONSTRAINT fk_msg_conversation FOREIGN KEY (conversation_id) REFERENCES conversations(id),
    CONSTRAINT fk_msg_sender FOREIGN KEY (sender_id) REFERENCES users(id),
    CONSTRAINT fk_msg_reply FOREIGN KEY (reply_to_id) REFERENCES messages(id)
);

CREATE INDEX idx_messages_conv_id ON messages(conversation_id, id);
CREATE INDEX idx_messages_expires_at ON messages(expires_at);

-- ============================================================
-- MESSAGE RECEIPTS
-- ============================================================
CREATE TABLE IF NOT EXISTS message_receipts (
    message_id    BIGINT      NOT NULL,
    user_id       BIGINT      NOT NULL,
    delivered_at  DATETIME(3),
    read_at       DATETIME(3),
    PRIMARY KEY (message_id, user_id),
    CONSTRAINT fk_receipt_message FOREIGN KEY (message_id) REFERENCES messages(id) ON DELETE CASCADE,
    CONSTRAINT fk_receipt_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- ============================================================
-- MESSAGE REACTIONS
-- ============================================================
CREATE TABLE IF NOT EXISTS message_reactions (
    message_id  BIGINT      NOT NULL,
    user_id     BIGINT      NOT NULL,
    emoji       VARCHAR(10) NOT NULL,
    PRIMARY KEY (message_id, user_id),
    CONSTRAINT fk_reaction_message FOREIGN KEY (message_id) REFERENCES messages(id) ON DELETE CASCADE,
    CONSTRAINT fk_reaction_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- ============================================================
-- GROUP KEYS (per-member wrapped group AES key)
-- ============================================================
CREATE TABLE IF NOT EXISTS group_keys (
    conversation_id      BIGINT          NOT NULL,
    user_id              BIGINT          NOT NULL,
    key_version          INT             NOT NULL,
    wrapped_key          TEXT            NOT NULL,
    wrapper_public_key   TEXT            NOT NULL,
    PRIMARY KEY (conversation_id, user_id, key_version),
    CONSTRAINT fk_gk_conversation FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE,
    CONSTRAINT fk_gk_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- ============================================================
-- REFRESH TOKENS
-- ============================================================
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id           BIGINT          NOT NULL AUTO_INCREMENT,
    user_id      BIGINT          NOT NULL,
    token_hash   VARCHAR(64)     NOT NULL,
    expires_at   DATETIME(3)     NOT NULL,
    revoked_at   DATETIME(3),
    replaced_by  BIGINT,
    created_at   DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_rt_token_hash (token_hash),
    CONSTRAINT fk_rt_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_rt_user_id ON refresh_tokens(user_id);

-- ============================================================
-- ATTACHMENTS
-- ============================================================
CREATE TABLE IF NOT EXISTS attachments (
    id               BIGINT          NOT NULL AUTO_INCREMENT,
    uploader_id      BIGINT          NOT NULL,
    conversation_id  BIGINT          NOT NULL,
    storage_path     VARCHAR(500)    NOT NULL,
    size             BIGINT          NOT NULL,
    mime_type        VARCHAR(100)    NOT NULL,
    encrypted_name   VARCHAR(500)    NOT NULL,
    created_at       DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    CONSTRAINT fk_att_uploader FOREIGN KEY (uploader_id) REFERENCES users(id),
    CONSTRAINT fk_att_conversation FOREIGN KEY (conversation_id) REFERENCES conversations(id)
);

-- ============================================================
-- BLOCKED USERS
-- ============================================================
CREATE TABLE IF NOT EXISTS blocked_users (
    user_id         BIGINT  NOT NULL,
    blocked_user_id BIGINT  NOT NULL,
    PRIMARY KEY (user_id, blocked_user_id),
    CONSTRAINT fk_bu_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_bu_blocked FOREIGN KEY (blocked_user_id) REFERENCES users(id) ON DELETE CASCADE
);
