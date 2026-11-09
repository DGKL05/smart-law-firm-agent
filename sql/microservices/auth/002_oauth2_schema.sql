USE smart_law_auth;

-- Spring Authorization Server 1.3.x JDBC schema for MySQL.
CREATE TABLE IF NOT EXISTS oauth2_registered_client (
    id VARCHAR(100) NOT NULL,
    client_id VARCHAR(100) NOT NULL,
    client_id_issued_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    client_secret VARCHAR(200) DEFAULT NULL,
    client_secret_expires_at TIMESTAMP NULL DEFAULT NULL,
    client_name VARCHAR(200) NOT NULL,
    client_authentication_methods VARCHAR(1000) NOT NULL,
    authorization_grant_types VARCHAR(1000) NOT NULL,
    redirect_uris VARCHAR(1000) DEFAULT NULL,
    post_logout_redirect_uris VARCHAR(1000) DEFAULT NULL,
    scopes VARCHAR(1000) NOT NULL,
    client_settings VARCHAR(2000) NOT NULL,
    token_settings VARCHAR(2000) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_oauth2_client_id (client_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS oauth2_authorization (
    id VARCHAR(100) NOT NULL,
    registered_client_id VARCHAR(100) NOT NULL,
    principal_name VARCHAR(200) NOT NULL,
    authorization_grant_type VARCHAR(100) NOT NULL,
    authorized_scopes VARCHAR(1000) DEFAULT NULL,
    attributes BLOB,
    state VARCHAR(500) DEFAULT NULL,
    authorization_code_value BLOB,
    authorization_code_issued_at TIMESTAMP NULL DEFAULT NULL,
    authorization_code_expires_at TIMESTAMP NULL DEFAULT NULL,
    authorization_code_metadata BLOB,
    access_token_value BLOB,
    access_token_issued_at TIMESTAMP NULL DEFAULT NULL,
    access_token_expires_at TIMESTAMP NULL DEFAULT NULL,
    access_token_metadata BLOB,
    access_token_type VARCHAR(100) DEFAULT NULL,
    access_token_scopes VARCHAR(1000) DEFAULT NULL,
    oidc_id_token_value BLOB,
    oidc_id_token_issued_at TIMESTAMP NULL DEFAULT NULL,
    oidc_id_token_expires_at TIMESTAMP NULL DEFAULT NULL,
    oidc_id_token_metadata BLOB,
    refresh_token_value BLOB,
    refresh_token_issued_at TIMESTAMP NULL DEFAULT NULL,
    refresh_token_expires_at TIMESTAMP NULL DEFAULT NULL,
    refresh_token_metadata BLOB,
    user_code_value BLOB,
    user_code_issued_at TIMESTAMP NULL DEFAULT NULL,
    user_code_expires_at TIMESTAMP NULL DEFAULT NULL,
    user_code_metadata BLOB,
    device_code_value BLOB,
    device_code_issued_at TIMESTAMP NULL DEFAULT NULL,
    device_code_expires_at TIMESTAMP NULL DEFAULT NULL,
    device_code_metadata BLOB,
    PRIMARY KEY (id),
    KEY idx_oauth2_access_token (access_token_value(128)),
    KEY idx_oauth2_authorization_code (authorization_code_value(128)),
    KEY idx_oauth2_access_token_expiry (access_token_expires_at),
    KEY idx_oauth2_client_principal (registered_client_id, principal_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS oauth2_authorization_consent (
    registered_client_id VARCHAR(100) NOT NULL,
    principal_name VARCHAR(200) NOT NULL,
    authorities VARCHAR(1000) NOT NULL,
    PRIMARY KEY (registered_client_id, principal_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
