-- V3__Create_gamification_tables.sql
-- Creates tables for missions, shop, and gamification system

-- Missions table
CREATE TABLE missions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    type VARCHAR(20) NOT NULL CHECK (type IN ('DAILY', 'WEEKLY', 'MONTHLY', 'ONE_TIME', 'ACHIEVEMENT')),
    difficulty VARCHAR(20) NOT NULL CHECK (difficulty IN ('EASY', 'MEDIUM', 'HARD', 'EXPERT')),
    xp_reward INTEGER NOT NULL DEFAULT 0,
    points_reward INTEGER NOT NULL DEFAULT 0,
    required_count INTEGER NOT NULL DEFAULT 1,
    spel_expression VARCHAR(500),
    start_date TIMESTAMP,
    end_date TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Mission progress table
CREATE TABLE mission_progress (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    mission_id UUID NOT NULL REFERENCES missions(id) ON DELETE CASCADE,
    current_count INTEGER NOT NULL DEFAULT 0,
    completed BOOLEAN NOT NULL DEFAULT false,
    completed_at TIMESTAMP,
    rewarded BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, mission_id)
);

-- Shop items table
CREATE TABLE shop_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    type VARCHAR(20) NOT NULL CHECK (type IN ('AVATAR_FRAME', 'BADGE', 'THEME', 'TITLE', 'EMOTE', 'BOOST')),
    price INTEGER NOT NULL,
    image_url VARCHAR(500),
    level_required INTEGER,
    active BOOLEAN NOT NULL DEFAULT true,
    stock_limit INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- User inventory table
CREATE TABLE user_inventory (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    shop_item_id UUID NOT NULL REFERENCES shop_items(id) ON DELETE CASCADE,
    equipped BOOLEAN NOT NULL DEFAULT false,
    acquired_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, shop_item_id)
);

-- Create indexes
CREATE INDEX idx_missions_type ON missions(type);
CREATE INDEX idx_missions_active ON missions(active);
CREATE INDEX idx_missions_dates ON missions(start_date, end_date);

CREATE INDEX idx_mission_progress_user_id ON mission_progress(user_id);
CREATE INDEX idx_mission_progress_mission_id ON mission_progress(mission_id);
CREATE INDEX idx_mission_progress_completed ON mission_progress(completed);

CREATE INDEX idx_shop_items_type ON shop_items(type);
CREATE INDEX idx_shop_items_active ON shop_items(active);

CREATE INDEX idx_user_inventory_user_id ON user_inventory(user_id);
CREATE INDEX idx_user_inventory_equipped ON user_inventory(user_id, equipped);

-- Create triggers for updated_at
CREATE TRIGGER update_missions_updated_at BEFORE UPDATE ON missions
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_mission_progress_updated_at BEFORE UPDATE ON mission_progress
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_shop_items_updated_at BEFORE UPDATE ON shop_items
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
