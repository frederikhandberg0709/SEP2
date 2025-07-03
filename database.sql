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

CREATE TABLE messages (
    id SERIAL PRIMARY KEY,
    room_id INTEGER, -- NULL for direct messages
    direct_chat_id INTEGER, -- NULL for group messages
    sender_username VARCHAR(50) NOT NULL,
    content TEXT NOT NULL,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    reply_to_message_id INTEGER,
    is_edited BOOLEAN DEFAULT FALSE,
    edited_timestamp TIMESTAMP,
    is_deleted BOOLEAN DEFAULT FALSE,

    CONSTRAINT fk_messages_room FOREIGN KEY (room_id)
        REFERENCES group_chats(id) ON DELETE CASCADE,
    CONSTRAINT fk_messages_direct_chat FOREIGN KEY (direct_chat_id)
        REFERENCES direct_chats(id) ON DELETE CASCADE,
    CONSTRAINT fk_messages_sender FOREIGN KEY (sender_username)
        REFERENCES users(username) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_messages_reply FOREIGN KEY (reply_to_message_id)
        REFERENCES messages(id) ON DELETE SET NULL,

    CONSTRAINT chk_message_chat_type CHECK (
        (room_id IS NOT NULL AND direct_chat_id IS NULL) OR
        (room_id IS NULL AND direct_chat_id IS NOT NULL)
    ),
    CONSTRAINT chk_content_length CHECK (LENGTH(TRIM(content)) >= 1)
);

CREATE INDEX idx_messages_room_timestamp ON messages(room_id, timestamp DESC)
    WHERE room_id IS NOT NULL AND is_deleted = FALSE;
CREATE INDEX idx_messages_direct_timestamp ON messages(direct_chat_id, timestamp DESC)
    WHERE direct_chat_id IS NOT NULL AND is_deleted = FALSE;
CREATE INDEX idx_messages_sender ON messages(sender_username);
CREATE INDEX idx_messages_reply ON messages(reply_to_message_id);

CREATE OR REPLACE FUNCTION update_direct_chat_last_message()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE direct_chats
    SET last_message_timestamp = NEW.timestamp
    WHERE id = NEW.direct_chat_id;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_update_direct_chat_last_message
    AFTER INSERT ON messages
    FOR EACH ROW
    WHEN (NEW.direct_chat_id IS NOT NULL)
    EXECUTE FUNCTION update_direct_chat_last_message();

-- User sessions table for tracking online status
CREATE TABLE user_sessions (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    session_token VARCHAR(255) NOT NULL UNIQUE,
    login_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_activity TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,

    CONSTRAINT fk_user_sessions_user FOREIGN KEY (username)
        REFERENCES users(username) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE INDEX idx_user_sessions_username ON user_sessions(username);
CREATE INDEX idx_user_sessions_token ON user_sessions(session_token);
CREATE INDEX idx_user_sessions_active ON user_sessions(is_active, last_activity);

-- Message reactions table
    -- WILL NOT BE IMPLEMENTED IN THIS ITERATION. MAYBE IN THE FUTURE!
/*CREATE TABLE message_reactions (
    id SERIAL PRIMARY KEY,
    message_id INTEGER NOT NULL,
    username VARCHAR(50) NOT NULL,
    reaction VARCHAR(50) NOT NULL, -- emoji or reaction type
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Foreign key constraints
    CONSTRAINT fk_message_reactions_message FOREIGN KEY (message_id)
        REFERENCES messages(id) ON DELETE CASCADE,
    CONSTRAINT fk_message_reactions_user FOREIGN KEY (username)
        REFERENCES users(username) ON DELETE CASCADE ON UPDATE CASCADE,

    -- Unique constraint (one reaction type per user per message)
    CONSTRAINT uk_message_reactions UNIQUE (message_id, username, reaction)
);*/

-- Indexes for message reactions
/*CREATE INDEX idx_message_reactions_message ON message_reactions(message_id);
CREATE INDEX idx_message_reactions_user ON message_reactions(username);*/

CREATE VIEW chat_overview AS
SELECT
    'direct' as chat_type,
    dc.id as chat_id,
    dc.user1_username || ' & ' || dc.user2_username as chat_name,
    dc.created_timestamp,
    dc.last_message_timestamp,
    m.content as last_message_content,
    m.sender_username as last_message_sender
FROM direct_chats dc
LEFT JOIN messages m ON dc.id = m.direct_chat_id
    AND m.timestamp = dc.last_message_timestamp
    AND m.is_deleted = FALSE

UNION ALL

SELECT
    'group' as chat_type,
    gc.id as chat_id,
    gc.name as chat_name,
    gc.created_timestamp,
    COALESCE(last_msg.last_timestamp, gc.created_timestamp) as last_message_timestamp,
    last_msg.content as last_message_content,
    last_msg.sender_username as last_message_sender
FROM group_chats gc
LEFT JOIN (
    SELECT
        room_id,
        MAX(timestamp) as last_timestamp,
        (array_agg(content ORDER BY timestamp DESC))[1] as content,
        (array_agg(sender_username ORDER BY timestamp DESC))[1] as sender_username
    FROM messages
    WHERE room_id IS NOT NULL AND is_deleted = FALSE
    GROUP BY room_id
) last_msg ON gc.id = last_msg.room_id;

-- Function to get user's accessible chats
CREATE OR REPLACE FUNCTION get_user_chats(p_username VARCHAR(50))
RETURNS TABLE (
    chat_type VARCHAR(10),
    chat_id INTEGER,
    chat_name VARCHAR(255),
    last_message_timestamp TIMESTAMP,
    unread_count INTEGER
) AS $$
BEGIN
    RETURN QUERY
    -- Direct chats
    SELECT
        'direct'::VARCHAR(10),
        dc.id,
        CASE
            WHEN dc.user1_username = p_username THEN dc.user2_username
            ELSE dc.user1_username
        END,
        dc.last_message_timestamp,
        0 -- TODO: Implement unread count logic
    FROM direct_chats dc
    WHERE (dc.user1_username = p_username OR dc.user2_username = p_username)
      AND NOT (
          (dc.user1_username = p_username AND dc.user1_archived = TRUE) OR
          (dc.user2_username = p_username AND dc.user2_archived = TRUE)
      )

    UNION ALL

    -- Group chats
    SELECT
        'group'::VARCHAR(10),
        gc.id,
        gc.name,
        COALESCE(last_msg.last_timestamp, gc.created_timestamp),
        0 -- TODO: Implement unread count logic
    FROM group_chats gc
    INNER JOIN group_members gm ON gc.id = gm.room_id
    LEFT JOIN (
        SELECT
            room_id,
            MAX(timestamp) as last_timestamp
        FROM messages
        WHERE room_id IS NOT NULL AND is_deleted = FALSE
        GROUP BY room_id
    ) last_msg ON gc.id = last_msg.room_id
    WHERE gm.username = p_username

    ORDER BY last_message_timestamp DESC NULLS LAST;
END;
$$ LANGUAGE plpgsql;
