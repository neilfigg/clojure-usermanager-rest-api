DROP TABLE IF EXISTS role CASCADE;
DROP TABLE IF EXISTS account CASCADE; -- NOTE: could not name this table user is a reserved word in Postgres!

CREATE TABLE account (
    id SERIAL NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    screen_name VARCHAR(50),
    email VARCHAR(100) NOT NULL,
    password VARCHAR(300) NOT NULL,
    token_key VARCHAR(100) NOT NULL,
    last_login_timestamp TIMESTAMP,
    failed_login_attempts integer DEFAULT 0,
    lockout_time TIMESTAMP,
    active BOOLEAN DEFAULT false NOT NULL,
    created_timestamp TIMESTAMP NOT NULL,
    created_user VARCHAR(100),
    modified_timestamp TIMESTAMP NOT NULL,
    modified_user VARCHAR(100),
    CONSTRAINT account_pkey PRIMARY KEY (id));

CREATE UNIQUE INDEX idx_acct_email
ON account(email);

CREATE TABLE role (
    id SERIAL NOT NULL,
    account_id integer NOT NULL,
    name VARCHAR(100) NOT NULL,
    active BOOLEAN DEFAULT false NOT NULL,
    created_timestamp TIMESTAMP NOT NULL,
    created_user VARCHAR(100),
    modified_timestamp TIMESTAMP NOT NULL,
    modified_user VARCHAR(100),
    CONSTRAINT role_pkey PRIMARY KEY (id),
    CONSTRAINT account_id_fkey FOREIGN KEY (account_id) REFERENCES account (id));

CREATE UNIQUE INDEX idx_role_acct_name
ON role(account_id, name);
