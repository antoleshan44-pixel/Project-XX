-- Flyway Migration V7: Performance Indexes for Multi-Tenant Querying & M-Pesa Deduplication

CREATE INDEX IF NOT EXISTS idx_properties_pm_approval ON properties (pm_account_id, approval_status);
CREATE INDEX IF NOT EXISTS idx_units_property_avail ON units (property_id, status, is_available, published);
CREATE INDEX IF NOT EXISTS idx_payments_mpesa_receipt ON payments (mpesa_receipt_number);
CREATE INDEX IF NOT EXISTS idx_leases_pm_status ON leases (pm_account_id, status);
CREATE INDEX IF NOT EXISTS idx_viewings_unit_status ON viewings (unit_id, status);
