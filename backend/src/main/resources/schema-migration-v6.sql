-- ════════════════════════════════════════════════════════════════════
--  ReDiscoverU v6 — Database Migration Script
--  Run this ONLY if upgrading from a previous version.
--  Fresh installs: no action needed (Hibernate 'update' handles it).
-- ════════════════════════════════════════════════════════════════════

-- 1. Drop old enrollment table (no longer needed)
DROP TABLE IF EXISTS user_programs;

-- 2. Remove per-program price column (now platform-level in platform_settings)
ALTER TABLE programs DROP COLUMN IF EXISTS price;

-- 3. Platform settings table (auto-created by Hibernate on first run)
CREATE TABLE IF NOT EXISTS platform_settings (
    id BIGINT PRIMARY KEY DEFAULT 1,
    lifetime_price DECIMAL(10,2) NOT NULL DEFAULT 499.00,
    platform_name  VARCHAR(500)
);

-- Insert default row if not exists
INSERT IGNORE INTO platform_settings (id, lifetime_price, platform_name)
VALUES (1, 499.00, 'ReDiscoverU');

-- 4. Remove program_id FK from payments (now platform-level payments)
ALTER TABLE payments DROP FOREIGN KEY IF EXISTS FKpayments_program;
ALTER TABLE payments DROP COLUMN IF EXISTS program_id;

-- 5. Ensure subscription_status enum is correct
ALTER TABLE users
    MODIFY COLUMN subscription_status ENUM('PENDING','PAID') NOT NULL DEFAULT 'PENDING';

-- ════════════════════════════════════════════════════════════════════
