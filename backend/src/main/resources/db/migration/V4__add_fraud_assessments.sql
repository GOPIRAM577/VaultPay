ALTER TABLE fraud_assessments RENAME COLUMN assessed_at TO created_at;

ALTER TABLE payments ADD COLUMN sender_user_id UUID;
ALTER TABLE payments ADD COLUMN receiver_user_id UUID;
ALTER TABLE payments DROP COLUMN IF EXISTS sender_wallet_id CASCADE;

CREATE INDEX idx_payments_sender_created ON payments (sender_user_id, created_at);
