COMMENT ON SCHEMA public IS '';
CREATE TABLE public.audit_logs (
    is_success boolean,
    created_at timestamp(6) without time zone,
    execution_time bigint,
    "timestamp" timestamp(6) without time zone,
    id uuid NOT NULL,
    action character varying(255) NOT NULL,
    correlation_id character varying(255),
    event_type character varying(255),
    ip_address character varying(255),
    resource_id character varying(255),
    resource_type character varying(255) NOT NULL,
    service_name character varying(255),
    user_agent character varying(255),
    user_id character varying(255) NOT NULL,
    username character varying(255),
    details jsonb,
    metadata jsonb
);
CREATE TABLE public.auth_users (
    email_verified boolean,
    is_active boolean,
    phone_verified boolean,
    created_at timestamp(6) without time zone,
    deleted_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    id uuid NOT NULL,
    pm_account_id uuid,
    email character varying(255) NOT NULL,
    first_name character varying(255) NOT NULL,
    last_name character varying(255) NOT NULL,
    password_hash character varying(255) NOT NULL,
    phone character varying(255),
    role character varying(255) NOT NULL,
    status character varying(255) NOT NULL,
    CONSTRAINT auth_users_role_check CHECK (((role)::text = ANY ((ARRAY['SUPER_ADMIN'::character varying, 'PM_ADMIN'::character varying, 'PM_STAFF'::character varying, 'TENANT'::character varying])::text[]))),
    CONSTRAINT auth_users_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'INACTIVE'::character varying, 'SUSPENDED'::character varying, 'DELETED'::character varying, 'PENDING_VERIFICATION'::character varying])::text[])))
);
CREATE TABLE public.crm_contacts (
    is_active boolean,
    created_at timestamp(6) without time zone NOT NULL,
    deleted_at timestamp(6) without time zone,
    last_contact_date timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    assigned_to uuid,
    id uuid NOT NULL,
    pm_account_id uuid NOT NULL,
    address character varying(255),
    city character varying(255),
    company character varying(255),
    country character varying(255),
    email character varying(255) NOT NULL,
    first_name character varying(255) NOT NULL,
    last_name character varying(255) NOT NULL,
    mobile character varying(255),
    notes text,
    phone character varying(255) NOT NULL,
    "position" character varying(255),
    preferred_contact_method character varying(255),
    source character varying(255),
    state character varying(255),
    type character varying(255) NOT NULL,
    zip_code character varying(255)
);
CREATE TABLE public.leases (
    is_active boolean,
    rent_amount numeric(10,2) NOT NULL,
    security_deposit numeric(10,2),
    created_at timestamp(6) without time zone,
    deleted_at timestamp(6) without time zone,
    end_date timestamp(6) without time zone NOT NULL,
    signed_at timestamp(6) without time zone,
    start_date timestamp(6) without time zone NOT NULL,
    terminated_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    id uuid NOT NULL,
    pm_account_id uuid NOT NULL,
    property_id uuid NOT NULL,
    tenant_id uuid,
    unit_id uuid NOT NULL,
    currency character varying(255) NOT NULL,
    payment_frequency character varying(255),
    status character varying(255) NOT NULL,
    termination_reason character varying(255),
    terms text,
    CONSTRAINT leases_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'ACTIVE'::character varying, 'EXPIRED'::character varying, 'TERMINATED'::character varying, 'CANCELLED'::character varying])::text[])))
);
CREATE TABLE public.listings (
    bathrooms integer,
    bedrooms integer,
    latitude double precision,
    longitude double precision,
    price double precision NOT NULL,
    published boolean NOT NULL,
    square_footage double precision,
    created_at timestamp(6) without time zone NOT NULL,
    deleted_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    id uuid NOT NULL,
    pm_account_id uuid NOT NULL,
    property_id uuid NOT NULL,
    unit_id uuid NOT NULL,
    address character varying(255),
    city character varying(255),
    country character varying(255),
    currency character varying(255),
    description text,
    photo_urls character varying(255),
    property_type character varying(255),
    state character varying(255),
    status character varying(255) NOT NULL,
    title character varying(255) NOT NULL,
    zip_code character varying(255)
);
CREATE TABLE public.maintenance_requests (
    completed_date timestamp(6) without time zone,
    created_at timestamp(6) without time zone NOT NULL,
    deleted_at timestamp(6) without time zone,
    resolved_at timestamp(6) without time zone,
    scheduled_date timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    assigned_to uuid,
    id uuid NOT NULL,
    pm_account_id uuid NOT NULL,
    property_id uuid NOT NULL,
    tenant_id uuid NOT NULL,
    unit_id uuid NOT NULL,
    description text,
    notes text,
    photo_url character varying(255),
    photo_urls character varying(255),
    priority character varying(255) NOT NULL,
    status character varying(255) NOT NULL,
    title character varying(255) NOT NULL,
    CONSTRAINT maintenance_requests_status_check CHECK (((status)::text = ANY ((ARRAY['OPEN'::character varying, 'IN_PROGRESS'::character varying, 'PENDING_PARTS'::character varying, 'RESOLVED'::character varying, 'CLOSED'::character varying, 'COMPLETED'::character varying, 'CANCELLED'::character varying, 'SUBMITTED'::character varying, 'ON_HOLD'::character varying])::text[])))
);
CREATE TABLE public.management_fees (
    amount numeric(38,2) NOT NULL,
    fee_rate numeric(38,2),
    is_active boolean,
    created_at timestamp(6) without time zone,
    deleted_at timestamp(6) without time zone,
    effective_date timestamp(6) without time zone,
    expiry_date timestamp(6) without time zone,
    invoiced_at timestamp(6) without time zone,
    paid_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    id uuid NOT NULL,
    pm_account_id uuid NOT NULL,
    property_id uuid NOT NULL,
    unit_id uuid,
    calculation_basis character varying(255),
    description text,
    fee_type character varying(255),
    status character varying(255) NOT NULL,
    period bytea NOT NULL
);
CREATE TABLE public.notifications (
    delivery_attempts integer,
    is_read boolean,
    created_at timestamp(6) without time zone NOT NULL,
    deleted_at timestamp(6) without time zone,
    read_at timestamp(6) without time zone,
    sent_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    id uuid NOT NULL,
    pm_account_id uuid,
    user_id uuid NOT NULL,
    channel character varying(255) NOT NULL,
    error_message character varying(255),
    message text NOT NULL,
    recipient character varying(255) NOT NULL,
    status character varying(255) NOT NULL,
    title character varying(255) NOT NULL,
    type character varying(255) NOT NULL
);
CREATE TABLE public.payment_proofs (
    amount_claimed numeric(38,2) NOT NULL,
    created_at timestamp(6) without time zone,
    resolved_at timestamp(6) without time zone,
    submitted_at timestamp(6) without time zone NOT NULL,
    id uuid NOT NULL,
    payment_id uuid,
    pm_account_id uuid NOT NULL,
    resolved_by uuid,
    tenant_id uuid NOT NULL,
    unit_id uuid,
    mpesa_receipt_number character varying(255),
    resolution_notes character varying(255),
    screenshot_url character varying(255),
    status character varying(255) NOT NULL,
    CONSTRAINT payment_proofs_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING_REVIEW'::character varying, 'MATCHED'::character varying, 'REJECTED'::character varying])::text[])))
);
CREATE TABLE public.payments (
    amount numeric(38,2) NOT NULL,
    amount_difference numeric(38,2),
    amount_expected numeric(38,2),
    is_reconciled boolean NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    deleted_at timestamp(6) without time zone,
    due_date timestamp(6) without time zone,
    payment_date timestamp(6) without time zone,
    reconciled_at timestamp(6) without time zone,
    transaction_date timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    id uuid NOT NULL,
    lease_id uuid NOT NULL,
    pm_account_id uuid NOT NULL,
    property_id uuid NOT NULL,
    tenant_id uuid NOT NULL,
    unit_id uuid NOT NULL,
    currency character varying(255),
    customer_name character varying(255),
    description character varying(255),
    mpesa_receipt_number character varying(255),
    payment_gateway character varying(255),
    payment_method character varying(255) NOT NULL,
    reference_number character varying(255) NOT NULL,
    status character varying(255) NOT NULL,
    transaction_id character varying(255),
    raw_payload oid,
    CONSTRAINT payments_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'PAID'::character varying, 'FAILED'::character varying, 'REFUNDED'::character varying, 'CANCELLED'::character varying, 'COMPLETED'::character varying, 'RECONCILED'::character varying, 'PARTIAL'::character varying, 'OVERPAID'::character varying, 'UNMATCHED'::character varying, 'MANAGEMENT_FEE'::character varying, 'PENDING_REVIEW'::character varying, 'MATCHED'::character varying, 'REJECTED'::character varying])::text[])))
);
CREATE TABLE public.pm_accounts (
    is_active boolean,
    created_at timestamp(6) without time zone,
    deleted_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    id uuid NOT NULL,
    company_name character varying(255) NOT NULL,
    service_option character varying(255) NOT NULL
);
CREATE TABLE public.properties (
    latitude double precision,
    longitude double precision,
    total_units integer NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    deleted_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    id uuid NOT NULL,
    owner_id uuid NOT NULL,
    pm_account_id uuid,
    address character varying(255) NOT NULL,
    amenities text,
    city character varying(255),
    country character varying(255),
    description character varying(255),
    location character varying(255),
    name character varying(255) NOT NULL,
    owner_email character varying(255),
    owner_name character varying(255),
    state character varying(255),
    status character varying(255) NOT NULL,
    type character varying(255) NOT NULL,
    zip_code character varying(255),
    CONSTRAINT properties_status_check CHECK (((status)::text = ANY ((ARRAY['AVAILABLE'::character varying, 'RENTED'::character varying, 'UNDER_MAINTENANCE'::character varying, 'PENDING_APPROVAL'::character varying, 'SOLD'::character varying, 'INACTIVE'::character varying])::text[])))
);
CREATE TABLE public.reports (
    created_at timestamp(6) without time zone NOT NULL,
    deleted_at timestamp(6) without time zone,
    expires_at timestamp(6) without time zone,
    file_size bigint,
    generated_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    generated_by uuid NOT NULL,
    id uuid NOT NULL,
    pm_account_id uuid NOT NULL,
    description text,
    error_message character varying(255),
    file_name character varying(255),
    file_path character varying(255),
    format character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    parameters character varying(255),
    status character varying(255) NOT NULL,
    type character varying(255) NOT NULL
);
CREATE TABLE public.tenants (
    credit_balance double precision,
    is_active boolean,
    created_at timestamp(6) without time zone,
    deleted_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    id uuid NOT NULL,
    pm_account_id uuid NOT NULL,
    unit_id uuid,
    user_id uuid,
    email character varying(255) NOT NULL,
    emergency_contact character varying(255),
    emergency_phone character varying(255),
    first_name character varying(255) NOT NULL,
    full_name character varying(255),
    id_number character varying(255),
    last_name character varying(255) NOT NULL,
    phone character varying(255) NOT NULL
);
CREATE TABLE public.unit_photo_urls (
    unit_id uuid NOT NULL,
    photo_url character varying(255)
);
CREATE TABLE public.units (
    bathrooms integer,
    bedrooms integer,
    floor integer NOT NULL,
    is_available boolean NOT NULL,
    published boolean NOT NULL,
    rent_amount double precision NOT NULL,
    square_footage double precision NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    deleted_at timestamp(6) without time zone,
    rented_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    current_tenant_id uuid,
    id uuid NOT NULL,
    property_id uuid,
    currency character varying(255),
    description character varying(255),
    features text,
    label character varying(255),
    status character varying(255) NOT NULL,
    unit_number character varying(255) NOT NULL,
    CONSTRAINT units_status_check CHECK (((status)::text = ANY ((ARRAY['AVAILABLE'::character varying, 'OCCUPIED'::character varying, 'PENDING'::character varying, 'MAINTENANCE'::character varying, 'UNDER_MAINTENANCE'::character varying, 'INACTIVE'::character varying, 'RENTED'::character varying, 'RESERVED'::character varying])::text[])))
);
ALTER TABLE ONLY public.audit_logs
    ADD CONSTRAINT audit_logs_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.auth_users
    ADD CONSTRAINT auth_users_email_key UNIQUE (email);
ALTER TABLE ONLY public.auth_users
    ADD CONSTRAINT auth_users_phone_key UNIQUE (phone);
ALTER TABLE ONLY public.auth_users
    ADD CONSTRAINT auth_users_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.crm_contacts
    ADD CONSTRAINT crm_contacts_email_key UNIQUE (email);
ALTER TABLE ONLY public.crm_contacts
    ADD CONSTRAINT crm_contacts_phone_key UNIQUE (phone);
ALTER TABLE ONLY public.crm_contacts
    ADD CONSTRAINT crm_contacts_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.leases
    ADD CONSTRAINT leases_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.listings
    ADD CONSTRAINT listings_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.maintenance_requests
    ADD CONSTRAINT maintenance_requests_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.management_fees
    ADD CONSTRAINT management_fees_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT notifications_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.payment_proofs
    ADD CONSTRAINT payment_proofs_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.payments
    ADD CONSTRAINT payments_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.pm_accounts
    ADD CONSTRAINT pm_accounts_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.properties
    ADD CONSTRAINT properties_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.reports
    ADD CONSTRAINT reports_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.tenants
    ADD CONSTRAINT tenants_email_key UNIQUE (email);
ALTER TABLE ONLY public.tenants
    ADD CONSTRAINT tenants_phone_key UNIQUE (phone);
ALTER TABLE ONLY public.tenants
    ADD CONSTRAINT tenants_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.units
    ADD CONSTRAINT units_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.unit_photo_urls
    ADD CONSTRAINT fk4mcnxbpkx8h0d2f7be46u6vnc FOREIGN KEY (unit_id) REFERENCES public.units(id);
ALTER TABLE ONLY public.leases
    ADD CONSTRAINT fkbvhw69fedufaa6on1xeibpvd0 FOREIGN KEY (tenant_id) REFERENCES public.tenants(id);
ALTER TABLE ONLY public.units
    ADD CONSTRAINT fksvctmn06wdb9r366vjpvf88j3 FOREIGN KEY (property_id) REFERENCES public.properties(id);
