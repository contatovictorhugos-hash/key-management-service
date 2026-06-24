CREATE TABLE crypto_keys (
    id UUID NOT NULL,
    public_key TEXT,
    private_key TEXT,
    is_active BOOLEAN,
    created_at TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_crypto_keys PRIMARY KEY (id)
);
