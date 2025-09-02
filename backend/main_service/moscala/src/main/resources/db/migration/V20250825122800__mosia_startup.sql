-- CreateEnum
CREATE TYPE WorkspaceMemberStatus AS ENUM ('Pending', 'NeedMoreSeat', 'NeedMoreSeatAndReview', 'UnderReview', 'Accepted', 'AllocatingSeat');

CREATE TYPE WorkspaceMemberSource AS ENUM ('Email', 'Link');

-- CreateDomain
CREATE DOMAIN MOSIAJSON AS JSON;
CREATE DOMAIN MOSIAJSONB AS JSONB;

-- CreateTable
CREATE TABLE users (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  email VARCHAR(255) UNIQUE NOT NULL,
  registered BOOLEAN DEFAULT TRUE NOT NULL,
  disabled BOOLEAN DEFAULT FALSE NOT NULL,
  name VARCHAR(100) NOT NULL,
  password_hash VARCHAR(255),
  avatar_url VARCHAR(255),
  email_verified TIMESTAMPTZ(3),
  created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL
);

ALTER TABLE users ADD CONSTRAINT chk_users_password_hash_format CHECK (password_hash LIKE '$2a$%');

CREATE TABLE user_connected_accounts (
                                        id UUID NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
                                        user_id UUID NOT NULL REFERENCES users ON UPDATE CASCADE ON DELETE CASCADE,
                                        provider VARCHAR NOT NULL,
                                        provider_account_id VARCHAR(36) NOT NULL,
                                        scope TEXT,
                                        access_token TEXT,
                                        refresh_token TEXT,
                                        expires_at TIMESTAMPTZ(3),
                                        created_at TIMESTAMPTZ(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                        updated_at TIMESTAMPTZ(3) NOT NULL
);

CREATE INDEX user_connected_accounts_user_id_idx ON user_connected_accounts (user_id);

CREATE INDEX user_connected_accounts_provider_account_id_idx ON user_connected_accounts (provider_account_id);

CREATE TABLE multiple_users_sessions (
                                               id UUID NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
                                               created_at TIMESTAMPTZ(3) DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE user_sessions (
                                      id UUID NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
                                      session_id UUID NOT NULL REFERENCES multiple_users_sessions ON UPDATE CASCADE ON DELETE CASCADE,
                                      user_id UUID NOT NULL REFERENCES users ON UPDATE CASCADE ON DELETE CASCADE,
                                      last_accessed_at TIMESTAMPTZ(3) NOT NULL,
                                      expires_at TIMESTAMPTZ(3) NOT NULL,
                                      created_at TIMESTAMPTZ(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                      is_active BOOLEAN DEFAULT true NOT NULL
);

CREATE UNIQUE INDEX user_sessions_session_id_user_id_key ON user_sessions (session_id, user_id);

CREATE TABLE verify_tokens (
                                    token UUID NOT NULL,
                                    type INTEGER NOT NULL,
                                    credential TEXT,
                                    expires_at TIMESTAMPTZ(3) NOT NULL
);

CREATE UNIQUE INDEX verify_tokens_type_token_key ON verify_tokens (type, token);

CREATE TABLE configs (
    id VARCHAR NOT NULL,
    value MOSIAJSONB NOT NULL,
    created_at TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ(3) NOT NULL,
    last_updated_by UUID,

    CONSTRAINT configs_pkey PRIMARY KEY (id)
);

ALTER TABLE configs ADD CONSTRAINT configs_last_updated_by_fkey FOREIGN KEY (last_updated_by) REFERENCES users(id) ON DELETE SET NULL ON UPDATE CASCADE;

CREATE TABLE features (
                                  id SERIAL PRIMARY KEY,
                                  feature VARCHAR NOT NULL,
                                  configs MOSIAJSON NOT NULL DEFAULT '{}',
                                  updated_at TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  created_at TIMESTAMPTZ(3) DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE user_features (
                                       id SERIAL PRIMARY KEY,
                                       user_id UUID NOT NULL REFERENCES users ON UPDATE CASCADE ON DELETE CASCADE,
                                       feature_id INTEGER NOT NULL REFERENCES features ON UPDATE CASCADE ON DELETE CASCADE,
                                       name VARCHAR NOT NULL DEFAULT '',
                                       type INTEGER NOT NULL DEFAULT 0,
                                       reason VARCHAR NOT NULL,
                                       created_at TIMESTAMPTZ(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                       expired_at TIMESTAMPTZ(3),
                                       activated BOOLEAN DEFAULT FALSE NOT NULL
);

CREATE INDEX user_features_user_id_idx ON user_features (user_id);

CREATE INDEX user_features_name_idx ON user_features(name);

CREATE INDEX user_features_feature_id_idx ON user_features(feature_id);

CREATE TABLE workspaces (
                           id UUID NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
                           public_space BOOLEAN NOT NULL,
                           name VARCHAR,
                           avatar_key VARCHAR,
                           enable_ai BOOLEAN NOT NULL DEFAULT true,
                           enable_url_preview BOOLEAN DEFAULT FALSE NOT NULL,
                           created_at TIMESTAMPTZ(3) DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE workspace_features (
                                            id SERIAL PRIMARY KEY,
                                            workspace_id UUID NOT NULL REFERENCES workspaces ON UPDATE CASCADE ON DELETE CASCADE,
                                            feature_id INTEGER NOT NULL REFERENCES features ON UPDATE CASCADE ON DELETE CASCADE,
                                            name VARCHAR NOT NULL DEFAULT '',
                                            type INTEGER NOT NULL DEFAULT 0,
                                            reason VARCHAR NOT NULL,
                                            configs MOSIAJSON NOT NULL DEFAULT '{}',
                                            created_at TIMESTAMPTZ(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                            expired_at TIMESTAMPTZ(3),
                                            activated BOOLEAN DEFAULT FALSE NOT NULL
);

CREATE UNIQUE INDEX features_feature_key ON features (feature);

CREATE INDEX workspace_features_name_idx ON workspace_features(name);

CREATE INDEX workspace_features_workspace_id_idx ON workspace_features(workspace_id);

CREATE INDEX workspace_features_feature_id_idx ON workspace_features(feature_id);

CREATE TABLE workspace_user_permissions (
                                       id UUID NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
                                       workspace_id UUID NOT NULL REFERENCES workspaces ON UPDATE CASCADE ON DELETE CASCADE,
                                       user_id UUID NOT NULL REFERENCES users ON UPDATE CASCADE ON DELETE CASCADE,
                                       inviter_id UUID,
                                       type INTEGER NOT NULL,
                                       status WorkspaceMemberStatus NOT NULL DEFAULT 'Pending',
                                       source WorkspaceMemberSource NOT NULL DEFAULT 'Email',
                                       accepted BOOLEAN DEFAULT FALSE NOT NULL,
                                       updated_at TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                       created_at TIMESTAMPTZ(3) DEFAULT CURRENT_TIMESTAMP NOT NULL
);


CREATE INDEX workspace_user_permissions_user_id_idx ON workspace_user_permissions (user_id);

CREATE UNIQUE INDEX workspace_user_permissions_workspace_id_user_id_key ON workspace_user_permissions (workspace_id, user_id);

