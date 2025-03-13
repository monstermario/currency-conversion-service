CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    api_key VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL
);

CREATE TABLE request_logs (
    id SERIAL PRIMARY KEY,
    api_key VARCHAR(255),
    from_currency VARCHAR(10),
    to_currency VARCHAR(10),
    amount DOUBLE PRECISION,
    converted_amount DOUBLE PRECISION,
    timestamp TIMESTAMP
);