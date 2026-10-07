-- Flyway Migration V9: add pm_account_id to audit_logs for tenant-scoped dashboard feed.
-- Existing rows keep NULL — they will not appear in PM feeds (correct: they're either
-- pre-tenant or system-level).

ALTER TABLE public.audit_logs
    ADD COLUMN IF NOT EXISTS pm_account_id character varying(255);

CREATE INDEX IF NOT EXISTS idx_audit_logs_pm_timestamp
    ON public.audit_logs (pm_account_id, "timestamp" DESC);

-- Backfill: for rows whose user_id can be resolved to a user with a pm_account_id,
-- set the column. Safe no-op for anonymous/auth rows.
UPDATE public.audit_logs al
SET pm_account_id = au.pm_account_id::text
FROM public.auth_users au
WHERE al.pm_account_id IS NULL
  AND al.user_id IS NOT NULL
  AND au.id::text = al.user_id
  AND au.pm_account_id IS NOT NULL;

-- Newsletter, blog, support tickets were already created in V8.
-- Add indexes for expected access patterns.
CREATE INDEX IF NOT EXISTS idx_newsletter_subscribers_email
    ON public.newsletter_subscribers (email);

CREATE INDEX IF NOT EXISTS idx_blog_posts_category_published
    ON public.blog_posts (category, published, published_at DESC);

CREATE INDEX IF NOT EXISTS idx_blog_posts_slug
    ON public.blog_posts (slug);

CREATE INDEX IF NOT EXISTS idx_support_tickets_user
    ON public.support_tickets (user_id, status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_support_tickets_pm
    ON public.support_tickets (pm_account_id, status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_support_ticket_replies_ticket
    ON public.support_ticket_replies (ticket_id, created_at ASC);