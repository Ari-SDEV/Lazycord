-- V4__Create_ranks_table.sql
-- Creates ranks table for configurable rank system

CREATE TABLE ranks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(50) NOT NULL UNIQUE,
    display_name VARCHAR(100) NOT NULL,
    description TEXT,
    min_level INTEGER NOT NULL,
    max_level INTEGER NOT NULL,
    badge_url VARCHAR(500),
    color_hex VARCHAR(7),
    sort_order INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT valid_level_range CHECK (min_level <= max_level)
);

-- Insert default ranks
INSERT INTO ranks (name, display_name, description, min_level, max_level, color_hex, sort_order) VALUES
('NEWBIE', 'Newbie', 'Just starting out', 1, 4, '#8B8B8B', 1),
('BRONZE', 'Bronze', 'Getting the hang of things', 5, 9, '#CD7F32', 2),
('SILVER', 'Silver', 'Experienced user', 10, 14, '#C0C0C0', 3),
('GOLD', 'Gold', 'Active community member', 15, 19, '#FFD700', 4),
('PLATINUM', 'Platinum', 'Dedicated contributor', 20, 29, '#E5E4E2', 5),
('DIAMOND', 'Diamond', 'Elite user', 30, 39, '#B9F2FF', 6),
('MASTER', 'Master', 'Expert level', 40, 49, '#FF6B6B', 7),
('LEGEND', 'Legend', 'True Lazycord legend', 50, 999, '#9B59B6', 8);

-- Create trigger for updated_at
CREATE TRIGGER update_ranks_updated_at BEFORE UPDATE ON ranks
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Create index
CREATE INDEX idx_ranks_sort_order ON ranks(sort_order);
CREATE INDEX idx_ranks_active ON ranks(active);
CREATE INDEX idx_ranks_level_range ON ranks(min_level, max_level);
