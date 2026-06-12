

--INITIAL-SCHEMA-V1--
   --REGINOTES--

--DONWLOAD EXTENSION
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
-- ENUMS
CREATE TYPE note_role AS ENUM ('EDITOR','VIEWER');
CREATE TYPE workspace_role AS ENUM ('OWNER','ADMIN','EDITOR','VIEWER');
CREATE TYPE search_status AS ENUM ('PENDING','INDEXED','FAILED');
CREATE TYPE notification_type AS ENUM (
    'WORKSPACE_INVITE',
    'NOTE_SHARED',
    'NOTE_UPDATED',
    'COLLABORATOR_JOINED',
    'VERSION_RESTORED');

--USER TABLE
CREATE TABLE app_users (
    id uuid primary key default gen_random_uuid(),
    email varchar(255) unique not null,
    display_name varchar(100) not null,
    password_hash varchar(255) not null,
    avatar_url varchar(500) ,
    is_active boolean default true,
    email_verified_at timestamp ,
    created_at timestamp not null default now(),
    updated_at timestamp not null default now()

);
--WORKSPACES
CREATE TABLE workspaces (
    id UUID primary key default gen_random_uuid(),
    name varchar(150) NOT NULL ,
    description TEXT,
    owner_id UUID  not null references app_users(id) ON DELETE RESTRICT,
    parent_id UUID references workspaces(id) ON DELETE SET NULL,
    icon_url varchar(500) ,
    settings jsonb ,
    created_at timestamp not null default now(),
    updated_at timestamp not null default now(),
    deleted_at timestamp
);
--REFRESH TOKENS
CREATE TABLE refresh_tokens(
    id UUID primary key default gen_random_uuid(),
    user_id UUID not null references app_users(id) ON DELETE CASCADE,
    token varchar(512) not null,
    user_agent varchar(500),
    ip_address varchar (45),
    revoked_at timestamp,
    expires_at timestamp not null,
    created_at timestamp not null default now()
);
--NOTIFICATIONS
CREATE TABLE notifications(
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references app_users(id) ON DELETE CASCADE,
    type notification_type not null,
    payload jsonb  not null default '{}',
    read boolean not null default false,
    created_at timestamp not null default now(),
    read_at timestamp
);
--NOTES
CREATE TABLE notes(
    id uuid primary key default gen_random_uuid(),
    workspace_id uuid not null references workspaces(id),
    owner_id uuid not null references app_users(id) ON DELETE RESTRICT,
    parent_id uuid references notes(id) ON DELETE CASCADE,
    title varchar not null default 'Empty Title',
    content jsonb not null default '{}',
    version integer not null default 1,
    search_status search_status not null default 'PENDING',
    is_pinned boolean not null default false,
    created_at timestamp not null default now(),
    updated_at timestamp not null default now(),
    deleted_at timestamp,
    indexed_at timestamp
);
--ATTACHMENTS
CREATE TABLE attachments(
    id uuid primary key default gen_random_uuid(),
    note_id uuid not null references notes(id) ON DELETE CASCADE,
    uploaded_by uuid references app_users(id) ON DELETE SET NULL,
    filename varchar(255) not null,
    mime_type varchar(100) not null,
    size_bytes BIGINT not null,
    storage_key varchar(500) not null unique,
    created_at timestamp not null default now()
);
--NOTE VERSIONING
CREATE TABLE note_versions(
    id uuid primary key default gen_random_uuid(),
    note_id uuid not null references notes(id) ON DELETE CASCADE,
    content jsonb not null default '{}',
    version INTEGER not null default 1,
    saved_by uuid references app_users(id) ON DELETE SET NULL ,
    created_at timestamp default now()
);
--TAGS
CREATE TABLE tags (
    id uuid primary key default gen_random_uuid(),
    workspace_id uuid not null references workspaces(id) ON DELETE CASCADE,
    name varchar(50) not null ,
    color varchar(7) not null default '#FFFFFF',
    created_at timestamp default now()
);
--NOTE_TAG RELATION
CREATE TABLE note_tags(
    note_id uuid not null references notes(id) ON DELETE CASCADE,
    tag_id uuid not null  references tags(id) ON DELETE CASCADE,
    primary key (note_id,tag_id)


);
--NOTE COLLAB
CREATE TABLE note_collaborators(
    id uuid primary key default gen_random_uuid(),
    note_id uuid not null  unique references notes(id) ON DELETE CASCADE,
    user_id uuid not null  unique references app_users(id) ON DELETE CASCADE,
    invited_by uuid references app_users(id) ON DELETE SET NULL,
    role note_role not null default 'VIEWER',
    added_at timestamp not null default now()
);
--WORKSPACE COLLAB
CREATE TABLE workspace_members(
    id uuid primary key default gen_random_uuid(),
    workspace_id uuid not null unique references workspaces(id) ON DELETE CASCADE,
    user_id uuid not null unique references app_users(id) ON DELETE CASCADE,
    invited_by uuid references app_users(id) ON DELETE SET NULL ,
    role workspace_role not null default 'VIEWER',
    joined_at timestamp  not null default now()

);

--INDEXES

-- users
CREATE INDEX idx_users_email       ON app_users(email);
CREATE INDEX idx_users_is_active   ON app_users(is_active) WHERE is_active = TRUE;

-- refresh_tokens
CREATE INDEX idx_refresh_tokens_user_id    ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_token      ON refresh_tokens(token);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);

-- workspaces
CREATE INDEX idx_workspaces_owner_id   ON workspaces(owner_id);
CREATE INDEX idx_workspaces_parent_id  ON workspaces(parent_id);
CREATE INDEX idx_workspaces_deleted_at ON workspaces(deleted_at) WHERE deleted_at IS NULL;

-- workspace_members
CREATE INDEX idx_workspace_members_workspace_id ON workspace_members(workspace_id);
CREATE INDEX idx_workspace_members_user_id      ON workspace_members(user_id);

-- notes
CREATE INDEX idx_notes_workspace_id   ON notes(workspace_id);
CREATE INDEX idx_notes_owner_id       ON notes(owner_id);
CREATE INDEX idx_notes_parent_id      ON notes(parent_id);
CREATE INDEX idx_notes_deleted_at     ON notes(deleted_at) WHERE deleted_at IS NULL;
CREATE INDEX idx_notes_search_status  ON notes(search_status) WHERE search_status = 'PENDING';
CREATE INDEX idx_notes_is_pinned      ON notes(workspace_id, is_pinned) WHERE is_pinned = TRUE;

-- note_collaborators
CREATE INDEX idx_note_collaborators_note_id  ON note_collaborators(note_id);
CREATE INDEX idx_note_collaborators_user_id  ON note_collaborators(user_id);

-- note_versions
CREATE INDEX idx_note_versions_note_id ON note_versions(note_id);

-- attachments
CREATE INDEX idx_attachments_note_id ON attachments(note_id);

-- notifications
CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_unread  ON notifications(user_id, read) WHERE read = FALSE;

-- tags
CREATE INDEX idx_tags_workspace_id ON tags(workspace_id);

-- note_tags
CREATE INDEX idx_note_tags_tag_id ON note_tags(tag_id);

--TRIGGER 1
--Update updated_at to now
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

--Call set_updated_at
CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON app_users
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

--TRIGGER 2
--Automatic Versioning
CREATE OR REPLACE FUNCTION snapshot_note_version()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.content IS DISTINCT FROM NEW.content THEN
       INSERT INTO note_versions (note_id, content, version, saved_by)
       VALUES (OLD.id, OLD.content, OLD.version, NEW.owner_id);

       NEW.version = OLD.version + 1;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_notes_version_snapshot
    BEFORE UPDATE ON notes
    FOR EACH ROW EXECUTE FUNCTION snapshot_note_version();

--TRIGGER 3

CREATE OR REPLACE FUNCTION mark_note_search_pending()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.title IS DISTINCT FROM NEW.title
    OR OLD.content IS DISTINCT FROM NEW.content
    THEN
       NEW.search_status = 'PENDING';
       NEW.indexed_at = NULL;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_notes_search_pending
    BEFORE UPDATE ON notes
    FOR EACH ROW EXECUTE FUNCTION mark_note_search_pending();

--TRIGGER 4

CREATE OR REPLACE FUNCTION add_owner_as_member()
RETURNS TRIGGER AS $$
BEGIN
INSERT INTO workspace_members (workspace_id, user_id, role)
VALUES (NEW.id, NEW.owner_id, 'OWNER');
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_workspace_add_owner
    AFTER INSERT ON workspaces
    FOR EACH ROW EXECUTE FUNCTION add_owner_as_member();



