-- ===================================================
-- Migration V13: Notification Enhancements & Schema
-- CivicPulse Smart City Command Centre
-- ===================================================

-- 1. Add incident link, severity, is_read boolean, and dedup_key
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS incident_id UUID REFERENCES incidents(id) ON DELETE SET NULL;
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS severity VARCHAR(50) NOT NULL DEFAULT 'MEDIUM';
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS is_read BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS dedup_key VARCHAR(255);

-- 2. Synchronize existing is_read values from status column
UPDATE notifications SET is_read = (status = 'READ') WHERE is_read IS NOT NULL;

-- 3. High-performance indexes for recipient filtering, unread counts, and deduplication
CREATE INDEX IF NOT EXISTS idx_notifications_recipient_user_id ON notifications(recipient_user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_is_read ON notifications(recipient_user_id, is_read);
CREATE INDEX IF NOT EXISTS idx_notifications_created_at ON notifications(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notifications_incident_id ON notifications(incident_id);
CREATE INDEX IF NOT EXISTS idx_notifications_dedup_key ON notifications(dedup_key);
