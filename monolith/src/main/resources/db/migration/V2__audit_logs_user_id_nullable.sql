-- Make user_id nullable so anonymous actions (login, register, OTP) can be audited.
-- Existing rows are preserved; new anonymous rows will have user_id = NULL.
ALTER TABLE public.audit_logs ALTER COLUMN user_id DROP NOT NULL;