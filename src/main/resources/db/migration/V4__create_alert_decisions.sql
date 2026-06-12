-- Append-only analyst decisions (REQ-11, ADR-0008): rows are never updated or
-- deleted - the table IS the audit trail. The alert's status column is merely a
-- projection of the newest entry here.
CREATE TABLE alert_decisions (
    id         UUID                     NOT NULL,
    alert_id   UUID                     NOT NULL,
    decision   VARCHAR(20)              NOT NULL,
    comment    VARCHAR(1000)            NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_alert_decisions PRIMARY KEY (id),
    CONSTRAINT fk_alert_decisions_alert FOREIGN KEY (alert_id) REFERENCES alerts (id)
);

-- Timeline view path: one alert's decisions, newest first.
CREATE INDEX idx_alert_decisions_alert ON alert_decisions (alert_id, created_at);
