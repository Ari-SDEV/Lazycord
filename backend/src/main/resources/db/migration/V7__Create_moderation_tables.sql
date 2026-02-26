-- V7__Create_moderation_tables.sql
-- Creates tables for moderation (bans, mutes, reports)

-- Channel bans table
CREATE TABLE channel_bans (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    channel_id UUID NOT NULL REFERENCES channels(id) ON DELETE CASCADE,
    reason TEXT,
    banned_by UUID NOT NULL REFERENCES users(id),
    banned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT true,
    unbanned_by UUID REFERENCES users(id),
    unbanned_at TIMESTAMP,
    unban_reason TEXT
);

-- Partial unique index for active bans
CREATE UNIQUE INDEX idx_channel_bans_active_unique ON channel_bans(user_id, channel_id) WHERE active = true;

-- Channel mutes table
CREATE TABLE channel_mutes (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    channel_id UUID NOT NULL REFERENCES channels(id) ON DELETE CASCADE,
    reason TEXT,
    muted_by UUID NOT NULL REFERENCES users(id),
    muted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT true,
    unmuted_by UUID REFERENCES users(id),
    unmuted_at TIMESTAMP
);

-- Partial unique index for active mutes
CREATE UNIQUE INDEX idx_channel_mutes_active_unique ON channel_mutes(user_id, channel_id) WHERE active = true;

-- Reports table
CREATE TABLE reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reporter_id UUID NOT NULL REFERENCES users(id),
    reported_user_id UUID NOT NULL REFERENCES users(id),
    channel_id UUID REFERENCES channels(id) ON DELETE SET NULL,
    message_id UUID REFERENCES messages(id) ON DELETE SET NULL,
    reason VARCHAR(20) NOT NULL CHECK (reason IN ('SPAM', 'HARASSMENT', 'INAPPROPRIATE_CONTENT', 'SCAM', 'OTHER')),
    details TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'UNDER_REVIEW', 'RESOLVED', 'DISMISSED')),
    resolved_by UUID REFERENCES users(id),
    resolved_at TIMESTAMP,
    resolution TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes
CREATE INDEX idx_channel_bans_channel ON channel_bans(channel_id) WHERE active = true;
CREATE INDEX idx_channel_bans_user ON channel_bans(user_id);
CREATE INDEX idx_channel_bans_expires ON channel_bans(expires_at) WHERE active = true;

CREATE INDEX idx_channel_mutes_channel ON channel_mutes(channel_id) WHERE active = true;
CREATE INDEX idx_channel_mutes_user ON channel_mutes(user_id);
CREATE INDEX idx_channel_mutes_expires ON channel_mutes(expires_at) WHERE active = true;

CREATE INDEX idx_reports_status ON reports(status);
CREATE INDEX idx_reports_reporter ON reports(reporter_id);
CREATE INDEX idx_reports_reported_user ON reports(reported_user_id);
CREATE INDEX idx_reports_created ON reports(created_at DESC);

-- Create trigger for updated_at on reports
CREATE TRIGGER update_reports_updated_at BEFORE UPDATE ON reports
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
