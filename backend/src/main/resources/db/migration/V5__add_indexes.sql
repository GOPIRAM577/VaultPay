CREATE UNIQUE INDEX IF NOT EXISTS idx_users_upi_handle ON users(upi_handle);
CREATE INDEX IF NOT EXISTS idx_users_phone_idx ON users(phone);
