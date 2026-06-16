-- Optional customer contact e-mail. This goes slightly beyond the PDF domain model
-- (which has no contact data); it is added as a realistic, additive extension to
-- demonstrate input validation. Nullable: existing customers and the core flow do
-- not require it. Length 254 = practical maximum local-part(64)+@+domain(253).
ALTER TABLE customers ADD COLUMN email VARCHAR(254);
