-- V5__Create_file_attachments.sql
-- Creates tables for file attachments

CREATE TABLE file_attachments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    filename VARCHAR(255) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    size BIGINT NOT NULL,
    storage_path VARCHAR(1000) NOT NULL,
    url VARCHAR(1000),
    uploaded_by UUID NOT NULL REFERENCES users(id),
    channel_id UUID REFERENCES channels(id),
    message_id UUID REFERENCES messages(id) ON DELETE CASCADE,
    file_hash VARCHAR(64),
    deleted BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes
CREATE INDEX idx_file_attachments_channel ON file_attachments(channel_id) WHERE deleted = false;
CREATE INDEX idx_file_attachments_message ON file_attachments(message_id) WHERE deleted = false;
CREATE INDEX idx_file_attachments_uploaded_by ON file_attachments(uploaded_by) WHERE deleted = false;
CREATE INDEX idx_file_attachments_file_hash ON file_attachments(file_hash) WHERE deleted = false;
CREATE INDEX idx_file_attachments_created_at ON file_attachments(created_at DESC) WHERE deleted = false;

-- Create unique constraint on file_hash for deduplication
CREATE UNIQUE INDEX idx_file_attachments_hash_unique ON file_attachments(file_hash) WHERE deleted = false;
