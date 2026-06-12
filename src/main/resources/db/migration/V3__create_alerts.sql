-- AML alerts: at most one per transaction, enforced by the database, not by
-- application memory (ADR-0007). The version column backs JPA optimistic locking
-- for concurrent analyst decisions (ADR-0008).
CREATE TABLE alerts (
    id             UUID                     NOT NULL,
    business_id    VARCHAR(64)              NOT NULL,
    transaction_id UUID                     NOT NULL,
    status         VARCHAR(20)              NOT NULL,
    reason         VARCHAR(255)             NOT NULL,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    version        BIGINT                   NOT NULL,
    CONSTRAINT pk_alerts PRIMARY KEY (id),
    CONSTRAINT uq_alerts_transaction UNIQUE (transaction_id),
    CONSTRAINT fk_alerts_transaction FOREIGN KEY (transaction_id) REFERENCES transactions (id)
);

CREATE INDEX idx_alerts_business_id ON alerts (business_id);
CREATE INDEX idx_alerts_status ON alerts (status);
