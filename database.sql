CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_username ON users(username);

CREATE TABLE direct_chats (
    id SERIAL PRIMARY KEY,
    user1_username VARCHAR(50) NOT NULL,
    user2_username VARCHAR(50) NOT NULL,
    created_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_message_timestamp TIMESTAMP,
    user1_archived BOOLEAN DEFAULT FALSE,
    user2_archived BOOLEAN DEFAULT FALSE,
    user1_blocked BOOLEAN DEFAULT FALSE,
    user2_blocked BOOLEAN DEFAULT FALSE,

    CONSTRAINT fk_direct_chats_user1 FOREIGN KEY (user1_username)
        REFERENCES users(username) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_direct_chats_user2 FOREIGN KEY (user2_username)
        REFERENCES users(username) ON DELETE CASCADE ON UPDATE CASCADE,

    -- Ensure user1 < user2 alphabetically for consistency
    CONSTRAINT chk_user_order CHECK (user1_username < user2_username),

    -- Unique constraint to prevent duplicate direct chats
    CONSTRAINT uk_direct_chats_users UNIQUE (user1_username, user2_username)
);

CREATE INDEX idx_direct_chats_user1 ON direct_chats(user1_username);
CREATE INDEX idx_direct_chats_user2 ON direct_chats(user2_username);
CREATE INDEX idx_direct_chats_last_message ON direct_chats(last_message_timestamp DESC);

CREATE TABLE group_chats (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    creator_username VARCHAR(50) NOT NULL,
    description TEXT,
    is_private BOOLEAN DEFAULT FALSE,
    max_members INTEGER DEFAULT 100,
    created_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_group_chats_creator FOREIGN KEY (creator_username)
        REFERENCES users(username) ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT chk_max_members CHECK (max_members > 0 AND max_members <= 1000),
    CONSTRAINT chk_name_length CHECK (LENGTH(TRIM(name)) >= 1)
);

CREATE INDEX idx_group_chats_creator ON group_chats(creator_username);
CREATE INDEX idx_group_chats_private ON group_chats(is_private);
CREATE INDEX idx_group_chats_created ON group_chats(created_timestamp DESC);

CREATE TABLE group_members (
    room_id INTEGER NOT NULL,
    username VARCHAR(50) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    joined_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    invited_by VARCHAR(50),
    is_muted BOOLEAN DEFAULT FALSE,
    mute_expiry TIMESTAMP,

    PRIMARY KEY (room_id, username),

    CONSTRAINT fk_group_members_room FOREIGN KEY (room_id)
        REFERENCES group_chats(id) ON DELETE CASCADE,
    CONSTRAINT fk_group_members_user FOREIGN KEY (username)
        REFERENCES users(username) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_group_members_inviter FOREIGN KEY (invited_by)
        REFERENCES users(username) ON DELETE SET NULL ON UPDATE CASCADE,

    CONSTRAINT chk_member_role CHECK (role IN ('MEMBER', 'ADMIN', 'CREATOR'))
);

CREATE INDEX idx_group_members_room ON group_members(room_id);
CREATE INDEX idx_group_members_user ON group_members(username);
CREATE INDEX idx_group_members_role ON group_members(room_id, role);
CREATE INDEX idx_group_members_joined ON group_members(joined_timestamp DESC);
