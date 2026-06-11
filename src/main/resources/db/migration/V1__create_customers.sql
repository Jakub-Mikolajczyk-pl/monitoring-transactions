-- Customers registered within a business context.
-- ADR-0003: business_id is a partition-style context key shared by many rows,
-- deliberately NOT unique. ADR-0011: ids are application-generated UUIDv7.
CREATE TABLE customers (
    id          UUID                     NOT NULL,
    business_id VARCHAR(64)              NOT NULL,
    first_name  VARCHAR(100)             NOT NULL,
    last_name   VARCHAR(100)             NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_customers PRIMARY KEY (id)
);

CREATE INDEX idx_customers_business_id ON customers (business_id);
