CREATE TABLE payment (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    fromAccount BIGINT REFERENCES account (id),
    toAccount BIGINT REFERENCES account (id),
    amount DECIMAL(12,4),
    date DATE
);
