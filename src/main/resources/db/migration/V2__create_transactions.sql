-- Immutable financial facts (ADR-0005). business_id is denormalised from the
-- customer's business context (ADR-0003) so the mandatory search filter and the
-- AML rules never need a join to customers.
CREATE TABLE transactions (
    id               UUID                     NOT NULL,
    business_id      VARCHAR(64)              NOT NULL,
    customer_id      UUID                     NOT NULL,
    amount           DECIMAL(19,2)            NOT NULL,
    currency         VARCHAR(3)               NOT NULL,
    transaction_date TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_transactions PRIMARY KEY (id),
    CONSTRAINT fk_transactions_customer FOREIGN KEY (customer_id) REFERENCES customers (id)
);

-- Search path (REQ-06): mandatory business context, optional date range.
CREATE INDEX idx_transactions_business_date ON transactions (business_id, transaction_date);

-- High-frequency rule path (REQ-10): one customer's transactions in a time window.
CREATE INDEX idx_transactions_customer_date ON transactions (customer_id, transaction_date);
