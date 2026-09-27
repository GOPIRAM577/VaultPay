DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name='ledger_entries' AND column_name='seq') THEN
        ALTER TABLE ledger_entries ADD COLUMN seq BIGSERIAL NOT NULL;
    END IF;
END $$;
