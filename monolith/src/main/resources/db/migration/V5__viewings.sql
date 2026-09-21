-- Commit 9: viewings (booking/viewing appointments for units)
--
-- A prospective renter (or an already-logged-in tenant) requests a viewing of
-- a specific unit at a specific datetime. The PM sees the request, and updates
-- its status (SCHEDULED / COMPLETED / CANCELLED).
--
-- No slot inventory / availability calendar in this commit — scheduledAt is
-- free-form, per the design decision. A real slots system is a later phase.
--
-- pm_account_id is denormalized from the unit's property at creation time so
-- PM-scoped queries are a single-table read without joins.

CREATE TABLE public.viewings (
                                 id                   uuid NOT NULL,
                                 unit_id              uuid NOT NULL,
                                 property_id          uuid NOT NULL,
                                 pm_account_id        uuid NOT NULL,
                                 requested_by_name    character varying(255) NOT NULL,
                                 requested_by_phone   character varying(255) NOT NULL,
                                 requested_by_user_id uuid,
                                 scheduled_at         timestamp(6) without time zone NOT NULL,
                                 status               character varying(32) NOT NULL,
                                 notes                text,
                                 created_at           timestamp(6) without time zone NOT NULL,
                                 updated_at           timestamp(6) without time zone,
                                 deleted_at           timestamp(6) without time zone
);

ALTER TABLE ONLY public.viewings
    ADD CONSTRAINT viewings_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.viewings
    ADD CONSTRAINT viewings_status_check CHECK (
    status::text = ANY (
    (ARRAY['SCHEDULED','COMPLETED','CANCELLED'])::text[]
    )
    );

CREATE INDEX idx_viewings_pm_account_id ON public.viewings (pm_account_id);
CREATE INDEX idx_viewings_unit_id       ON public.viewings (unit_id);
CREATE INDEX idx_viewings_user_id       ON public.viewings (requested_by_user_id);
CREATE INDEX idx_viewings_scheduled_at  ON public.viewings (scheduled_at);

-- No FK to units or properties — consistent with the rest of the codebase's
-- use of raw UUID references rather than JPA relationships across aggregates.