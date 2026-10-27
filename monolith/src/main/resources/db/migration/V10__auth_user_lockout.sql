-- Flyway Migration V10: add failed_login_attempts and lockout_until to auth_users for login hardening
ALTER TABLE public.auth_users
    ADD COLUMN IF NOT EXISTS failed_login_attempts integer DEFAULT 0,
    ADD COLUMN IF NOT EXISTS lockout_until timestamp without time zone;
