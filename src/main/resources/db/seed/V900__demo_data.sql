-- Demo dataset so the application is useful the moment it starts (no manual setup
-- before clicking around the UI). It lives in classpath:db/seed - a Flyway location
-- separate from the schema migrations - and is excluded from the test profile, so
-- it never pollutes the test database. The V9xx range is reserved for seed data,
-- keeping it clearly apart from the V1..V8 schema track.
--
-- Flyway runs below the application layer, so it cannot fire the asynchronous AML
-- engine. The alerts below are therefore inserted directly, set to exactly the
-- state the engine WOULD have produced for these transactions (see the rule docs).
-- Registering the same transactions through POST /api/transactions would trigger
-- the live pipeline instead.

-- Customers ------------------------------------------------------------------
-- DEMO-BANK is one business context; DEMO-FX is a second one, to show that
-- businessId partitions data (ADR-0003).
INSERT INTO customers (id, business_id, first_name, last_name, created_at) VALUES
 ('01900000-0000-7000-8000-0000000000c1', 'DEMO-BANK', 'Jan',   'Demczak',  TIMESTAMP WITH TIME ZONE '2026-06-10 08:00:00+00'),
 ('01900000-0000-7000-8000-0000000000c2', 'DEMO-BANK', 'Anna',  'Wzorcowa', TIMESTAMP WITH TIME ZONE '2026-06-10 08:01:00+00'),
 ('01900000-0000-7000-8000-0000000000c3', 'DEMO-BANK', 'Piotr', 'Czysty',   TIMESTAMP WITH TIME ZONE '2026-06-10 08:02:00+00'),
 ('01900000-0000-7000-8000-0000000000c4', 'DEMO-BANK', 'Zofia', 'Mieszana', TIMESTAMP WITH TIME ZONE '2026-06-10 08:03:00+00'),
 ('01900000-0000-7000-8000-0000000000c5', 'DEMO-FX',   'Marek', 'Walutowy', TIMESTAMP WITH TIME ZONE '2026-06-10 08:04:00+00');

-- Transactions ---------------------------------------------------------------
-- Jan: one ordinary payment (no alert) and one above the 2000 threshold.
INSERT INTO transactions (id, business_id, customer_id, amount, currency, transaction_date, created_at) VALUES
 ('01900000-0000-7000-8000-000000000a01', 'DEMO-BANK', '01900000-0000-7000-8000-0000000000c1',  150.00, 'PLN', TIMESTAMP WITH TIME ZONE '2026-06-14 08:00:00+00', TIMESTAMP WITH TIME ZONE '2026-06-14 08:00:01+00'),
 ('01900000-0000-7000-8000-000000000a02', 'DEMO-BANK', '01900000-0000-7000-8000-0000000000c1', 7500.00, 'PLN', TIMESTAMP WITH TIME ZONE '2026-06-14 09:00:00+00', TIMESTAMP WITH TIME ZONE '2026-06-14 09:00:01+00');

-- Anna: six payments inside one hour - the sixth crosses the >5/1h frequency rule.
INSERT INTO transactions (id, business_id, customer_id, amount, currency, transaction_date, created_at) VALUES
 ('01900000-0000-7000-8000-000000000b01', 'DEMO-BANK', '01900000-0000-7000-8000-0000000000c2', 99.00, 'PLN', TIMESTAMP WITH TIME ZONE '2026-06-15 09:00:00+00', TIMESTAMP WITH TIME ZONE '2026-06-15 09:00:01+00'),
 ('01900000-0000-7000-8000-000000000b02', 'DEMO-BANK', '01900000-0000-7000-8000-0000000000c2', 99.00, 'PLN', TIMESTAMP WITH TIME ZONE '2026-06-15 09:05:00+00', TIMESTAMP WITH TIME ZONE '2026-06-15 09:05:01+00'),
 ('01900000-0000-7000-8000-000000000b03', 'DEMO-BANK', '01900000-0000-7000-8000-0000000000c2', 99.00, 'PLN', TIMESTAMP WITH TIME ZONE '2026-06-15 09:10:00+00', TIMESTAMP WITH TIME ZONE '2026-06-15 09:10:01+00'),
 ('01900000-0000-7000-8000-000000000b04', 'DEMO-BANK', '01900000-0000-7000-8000-0000000000c2', 99.00, 'PLN', TIMESTAMP WITH TIME ZONE '2026-06-15 09:15:00+00', TIMESTAMP WITH TIME ZONE '2026-06-15 09:15:01+00'),
 ('01900000-0000-7000-8000-000000000b05', 'DEMO-BANK', '01900000-0000-7000-8000-0000000000c2', 99.00, 'PLN', TIMESTAMP WITH TIME ZONE '2026-06-15 09:20:00+00', TIMESTAMP WITH TIME ZONE '2026-06-15 09:20:01+00'),
 ('01900000-0000-7000-8000-000000000b06', 'DEMO-BANK', '01900000-0000-7000-8000-0000000000c2', 99.00, 'PLN', TIMESTAMP WITH TIME ZONE '2026-06-15 09:25:00+00', TIMESTAMP WITH TIME ZONE '2026-06-15 09:25:01+00');

-- Zofia: five small payments plus a large sixth - trips BOTH rules at once.
INSERT INTO transactions (id, business_id, customer_id, amount, currency, transaction_date, created_at) VALUES
 ('01900000-0000-7000-8000-000000000c01', 'DEMO-BANK', '01900000-0000-7000-8000-0000000000c4',   50.00, 'PLN', TIMESTAMP WITH TIME ZONE '2026-06-15 11:00:00+00', TIMESTAMP WITH TIME ZONE '2026-06-15 11:00:01+00'),
 ('01900000-0000-7000-8000-000000000c02', 'DEMO-BANK', '01900000-0000-7000-8000-0000000000c4',   50.00, 'PLN', TIMESTAMP WITH TIME ZONE '2026-06-15 11:05:00+00', TIMESTAMP WITH TIME ZONE '2026-06-15 11:05:01+00'),
 ('01900000-0000-7000-8000-000000000c03', 'DEMO-BANK', '01900000-0000-7000-8000-0000000000c4',   50.00, 'PLN', TIMESTAMP WITH TIME ZONE '2026-06-15 11:10:00+00', TIMESTAMP WITH TIME ZONE '2026-06-15 11:10:01+00'),
 ('01900000-0000-7000-8000-000000000c04', 'DEMO-BANK', '01900000-0000-7000-8000-0000000000c4',   50.00, 'PLN', TIMESTAMP WITH TIME ZONE '2026-06-15 11:15:00+00', TIMESTAMP WITH TIME ZONE '2026-06-15 11:15:01+00'),
 ('01900000-0000-7000-8000-000000000c05', 'DEMO-BANK', '01900000-0000-7000-8000-0000000000c4',   50.00, 'PLN', TIMESTAMP WITH TIME ZONE '2026-06-15 11:20:00+00', TIMESTAMP WITH TIME ZONE '2026-06-15 11:20:01+00'),
 ('01900000-0000-7000-8000-000000000c06', 'DEMO-BANK', '01900000-0000-7000-8000-0000000000c4', 5000.00, 'PLN', TIMESTAMP WITH TIME ZONE '2026-06-15 11:25:00+00', TIMESTAMP WITH TIME ZONE '2026-06-15 11:25:01+00');

-- Piotr: a single ordinary payment - stays clean, no alert.
INSERT INTO transactions (id, business_id, customer_id, amount, currency, transaction_date, created_at) VALUES
 ('01900000-0000-7000-8000-000000000d01', 'DEMO-BANK', '01900000-0000-7000-8000-0000000000c3', 300.00, 'PLN', TIMESTAMP WITH TIME ZONE '2026-06-13 10:00:00+00', TIMESTAMP WITH TIME ZONE '2026-06-13 10:00:01+00');

-- Marek: a payment in the DEMO-FX context (different currency, no alert).
INSERT INTO transactions (id, business_id, customer_id, amount, currency, transaction_date, created_at) VALUES
 ('01900000-0000-7000-8000-000000000e01', 'DEMO-FX', '01900000-0000-7000-8000-0000000000c5', 100.00, 'EUR', TIMESTAMP WITH TIME ZONE '2026-06-12 10:00:00+00', TIMESTAMP WITH TIME ZONE '2026-06-12 10:00:01+00');

-- Alerts (the engine's would-be output) -------------------------------------
-- a1: Jan's large payment -> suspicious amount, still open.
-- a2: Anna's sixth payment -> high frequency, still open.
-- a3: Zofia's large sixth payment -> both reasons merged into one alert, already
--     reviewed (APPROVED, version 1 because one decision has been applied).
INSERT INTO alerts (id, business_id, transaction_id, status, reason, created_at, version) VALUES
 ('01900000-0000-7000-8000-00000000a101', 'DEMO-BANK', '01900000-0000-7000-8000-000000000a02', 'OPEN',     'SUSPICIOUS_AMOUNT',                  TIMESTAMP WITH TIME ZONE '2026-06-14 09:00:05+00', 0),
 ('01900000-0000-7000-8000-00000000a102', 'DEMO-BANK', '01900000-0000-7000-8000-000000000b06', 'OPEN',     'HIGH_FREQUENCY',                     TIMESTAMP WITH TIME ZONE '2026-06-15 09:25:05+00', 0),
 ('01900000-0000-7000-8000-00000000a103', 'DEMO-BANK', '01900000-0000-7000-8000-000000000c06', 'APPROVED', 'SUSPICIOUS_AMOUNT,HIGH_FREQUENCY',   TIMESTAMP WITH TIME ZONE '2026-06-15 11:25:05+00', 1);

-- Decision history for the reviewed alert (append-only, ADR-0008).
INSERT INTO alert_decisions (id, alert_id, decision, comment, created_at) VALUES
 ('01900000-0000-7000-8000-00000000d101', '01900000-0000-7000-8000-00000000a103', 'APPROVE', 'Zweryfikowano z klientem - cykliczna wypłata z lokaty, transakcje legalne.', TIMESTAMP WITH TIME ZONE '2026-06-15 12:00:00+00');
