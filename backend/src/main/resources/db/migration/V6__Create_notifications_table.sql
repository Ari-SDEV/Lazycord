-- V6__Create_notifications_table.sql
-- Creates table for push notifications

CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(20) NOT NULL CHECK (type IN ('MENTION', 'MESSAGE', 'MISSION_COMPLETE', 'LEVEL_UP', 'SYSTEM')),
    title VARCHAR(255) NOT NULL,
    message TEXT,
    data_json TEXT,
    read BOOLEAN NOT NULL DEFAULT false,
    read_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes
CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_user_read ON notifications(user_id, read) WHERE read = false;
CREATE INDEX idx_notifications_created_at ON notifications(user_id, created_at DESC);
CREATE INDEX idx_notifications_type ON notifications(user_id, type) WHERE read = false;
