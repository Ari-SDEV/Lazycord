-- V8__Create_communities_multitenancy.sql
-- Creates Community entity and updates existing tables for multitenancy

-- Enable pgcrypto extension for gen_random_bytes
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Communities table
CREATE TABLE communities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    embed_id UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) UNIQUE,
    description TEXT,
    avatar_url VARCHAR(500),
    owner_id UUID NOT NULL REFERENCES users(id),
    api_key VARCHAR(255) NOT NULL,
    allowed_domains TEXT,
    is_public BOOLEAN NOT NULL DEFAULT false,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Community members table
CREATE TABLE community_members (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    community_id UUID NOT NULL REFERENCES communities(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL DEFAULT 'MEMBER' CHECK (role IN ('OWNER', 'ADMIN', 'MODERATOR', 'MEMBER')),
    active BOOLEAN NOT NULL DEFAULT true,
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, community_id)
);

-- Add community_id to existing tables
ALTER TABLE channels ADD COLUMN community_id UUID REFERENCES communities(id);
ALTER TABLE messages ADD COLUMN community_id UUID REFERENCES communities(id);
ALTER TABLE missions ADD COLUMN community_id UUID REFERENCES communities(id);
ALTER TABLE shop_items ADD COLUMN community_id UUID REFERENCES communities(id);
ALTER TABLE ranks ADD COLUMN community_id UUID REFERENCES communities(id);

-- Create default community for existing data
INSERT INTO communities (id, embed_id, name, slug, description, owner_id, api_key, is_public, active)
SELECT 
    gen_random_uuid(),
    gen_random_uuid(),
    'Default Community',
    'default',
    'Default community for existing data',
    (SELECT id FROM users ORDER BY created_at LIMIT 1),
    encode(gen_random_bytes(32), 'hex'),
    true,
    true
WHERE EXISTS (SELECT 1 FROM users);

-- Update existing data to default community
UPDATE channels SET community_id = (SELECT id FROM communities WHERE slug = 'default') WHERE community_id IS NULL;
UPDATE messages SET community_id = (SELECT id FROM communities WHERE slug = 'default') WHERE community_id IS NULL;
UPDATE missions SET community_id = (SELECT id FROM communities WHERE slug = 'default') WHERE community_id IS NULL;
UPDATE shop_items SET community_id = (SELECT id FROM communities WHERE slug = 'default') WHERE community_id IS NULL;
UPDATE ranks SET community_id = (SELECT id FROM communities WHERE slug = 'default') WHERE community_id IS NULL;

-- Make community_id NOT NULL after migration
ALTER TABLE channels ALTER COLUMN community_id SET NOT NULL;
ALTER TABLE missions ALTER COLUMN community_id SET NOT NULL;
ALTER TABLE shop_items ALTER COLUMN community_id SET NOT NULL;
ALTER TABLE ranks ALTER COLUMN community_id SET NOT NULL;

-- Create indexes
CREATE INDEX idx_communities_embed_id ON communities(embed_id);
CREATE INDEX idx_communities_slug ON communities(slug);
CREATE INDEX idx_communities_owner ON communities(owner_id);
CREATE INDEX idx_communities_public ON communities(is_public, active) WHERE is_public = true AND active = true;

CREATE INDEX idx_community_members_community ON community_members(community_id);
CREATE INDEX idx_community_members_user ON community_members(user_id);
CREATE INDEX idx_community_members_active ON community_members(community_id, active) WHERE active = true;

CREATE INDEX idx_channels_community ON channels(community_id);
CREATE INDEX idx_messages_community ON messages(community_id);
CREATE INDEX idx_missions_community ON missions(community_id);
CREATE INDEX idx_shop_items_community ON shop_items(community_id);
CREATE INDEX idx_ranks_community ON ranks(community_id);

-- Create trigger for updated_at
CREATE TRIGGER update_communities_updated_at BEFORE UPDATE ON communities
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
