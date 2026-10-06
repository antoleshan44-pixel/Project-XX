-- V6: Platform-level approval/suspension state for PM accounts and properties.
--
-- Model B: PMs register as ACTIVE (no approval gate at registration).
-- A SUPER_ADMIN can still suspend/reactivate a PM account, and every
-- property a PM creates starts PENDING_APPROVAL and is hidden from
-- tenant-facing listings until a SUPER_ADMIN approves it.
--
-- Existing rows are backfilled to their "safe" state so nothing breaks:
--   - pm_accounts.approval_status   -> 'ACTIVE'
--   - properties.approval_status    -> 'APPROVED' (so nothing disappears
--     from public listing during the rollout)

-- ============================================================
-- pm_accounts
-- ============================================================
ALTER TABLE public.pm_accounts
    ADD COLUMN approval_status    character varying(32),
    ADD COLUMN suspended_reason   character varying(512),
    ADD COLUMN suspended_at       timestamp(6) without time zone,
    ADD COLUMN suspended_by       uuid;

UPDATE public.pm_accounts
SET approval_status = 'ACTIVE'
WHERE approval_status IS NULL;

ALTER TABLE public.pm_accounts
    ALTER COLUMN approval_status SET NOT NULL;

ALTER TABLE public.pm_accounts
    ADD CONSTRAINT pm_accounts_approval_status_check CHECK (
        approval_status::text = ANY (
    (ARRAY['ACTIVE','SUSPENDED'])::text[]
    )
    );

-- ============================================================
-- properties
-- ============================================================
ALTER TABLE public.properties
    ADD COLUMN approval_status    character varying(32),
    ADD COLUMN approved_at        timestamp(6) without time zone,
    ADD COLUMN approved_by        uuid,
    ADD COLUMN rejection_reason   character varying(512),
    ADD COLUMN suspended_reason   character varying(512);

UPDATE public.properties
SET approval_status = 'APPROVED',
    approved_at     = created_at
WHERE approval_status IS NULL;

ALTER TABLE public.properties
    ALTER COLUMN approval_status SET NOT NULL;

ALTER TABLE public.properties
    ADD CONSTRAINT properties_approval_status_check CHECK (
        approval_status::text = ANY (
    (ARRAY['PENDING_APPROVAL','APPROVED','REJECTED','SUSPENDED'])::text[]
    )
    );

CREATE INDEX idx_properties_approval_status
    ON public.properties (approval_status);

CREATE INDEX idx_pm_accounts_approval_status
    ON public.pm_accounts (approval_status);