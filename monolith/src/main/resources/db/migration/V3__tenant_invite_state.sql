-- Commit 6b: tenant invite state machine
--
-- Adds three columns to tenants to track the invite lifecycle:
--   invite_status : PENDING | ACTIVATED | MANUAL (nullable for legacy rows)
--   invited_at    : when the most recent invite was sent
--   activated_at  : when the invite was accepted
--
-- Rationale (from the design decision on this commit): the 6-digit invite code
-- lives in Redis with a 10-minute TTL and can be regenerated freely. The
-- authoritative invite state must persist in Postgres so a Redis eviction does
-- not orphan a pending tenant.
--
-- Existing rows are backfilled:
--   - rows with user_id set are treated as already ACTIVATED
--   - rows without user_id are treated as MANUAL (PM created them directly,
--     no invite flow was involved)

ALTER TABLE public.tenants
    ADD COLUMN invite_status character varying(32),
    ADD COLUMN invited_at   timestamp(6) without time zone,
    ADD COLUMN activated_at timestamp(6) without time zone;

UPDATE public.tenants
SET invite_status = 'ACTIVATED',
    activated_at  = created_at
WHERE user_id IS NOT NULL
  AND invite_status IS NULL;

UPDATE public.tenants
SET invite_status = 'MANUAL'
WHERE invite_status IS NULL;

ALTER TABLE public.tenants
    ALTER COLUMN invite_status SET NOT NULL;