<#
.SYNOPSIS
    Seeds the Urbano Homes database with demo data:
    - 2 landlords (PM_ADMIN + pm_accounts row)
    - 3 tenants
    - 4 properties (2 per landlord)
    - 12 units (3 per property)
    - 3 active leases (1 per tenant)

.PARAMETER Reset
    Delete only the seeded rows before inserting. Identifies them by email
    suffix "@seed.urbano" (users/tenants) and "Seed Ltd" (company_name).

.PARAMETER Password
    Shared plaintext password for all seeded users. Default: Password123!

.EXAMPLE
    $env:DB_PASSWORD = "..."
    .\seed-db.ps1
    .\seed-db.ps1 -Reset
#>

param(
    [switch]$Reset,
    [string]$Password = "Password123!"
)

$ErrorActionPreference = "Stop"

# ============================================================
# 1. Config — pulled from env vars (matches application.yml)
# ============================================================
$dbHost    = $env:DB_HOST
$dbPort    = if ($env:DB_PORT) { $env:DB_PORT } else { "15692" }
$dbName    = if ($env:DB_NAME) { $env:DB_NAME } else { "defaultdb" }
$dbUser    = if ($env:DB_USER) { $env:DB_USER } else { "avnadmin" }
$dbPass    = $env:DB_PASSWORD
$dbSslMode = if ($env:DB_SSLMODE) { $env:DB_SSLMODE } else { "require" }

if (-not $dbHost) { throw "DB_HOST is not set" }
if (-not $dbPass) { throw "DB_PASSWORD is not set (rotate it first!)" }

# ============================================================
# 2. BCrypt hash of the shared password
# ------------------------------------------------------------
# Pre-computed hash of "Password123!" at cost 10.
# If you change -Password, replace this with a fresh hash from
# your running app (POST /api/auth/register then copy from DB).
# ============================================================
$bcryptHash = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy'

if ($Password -ne "Password123!") {
    throw "Only the default password 'Password123!' is supported by the pre-computed hash. To use a custom password, generate its BCrypt hash and edit the script."
}

# ============================================================
# 3. psql helper
# ============================================================
$env:PGPASSWORD = $dbPass

function Invoke-Psql {
    param([string]$Sql)
    $args = @(
        "-h", $dbHost,
        "-p", $dbPort,
        "-U", $dbUser,
        "-d", $dbName,
        "-v", "ON_ERROR_STOP=1",
        "-t", "-A",
        "-c", $Sql
    )
    $env:PGSSLMODE = $dbSslMode
    $out = & psql @args 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Host "psql failed:`n$out" -ForegroundColor Red
        throw "SQL failed: $Sql"
    }
    return $out
}

function Invoke-SqlFile {
    param([string]$Path)
    $args = @(
        "-h", $dbHost,
        "-p", $dbPort,
        "-U", $dbUser,
        "-d", $dbName,
        "-v", "ON_ERROR_STOP=1",
        "-f", $Path
    )
    $env:PGSSLMODE = $dbSslMode
    $out = & psql @args 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Host "psql failed:`n$out" -ForegroundColor Red
        throw "SQL file failed: $Path"
    }
    return $out
}

# ============================================================
# 4. Optional reset — delete only seeded rows
# ============================================================
if ($Reset) {
    Write-Host "🧹 Resetting previously seeded data..." -ForegroundColor Yellow

    # Order matters due to FKs (leases → tenants → units → properties → pm_accounts → users)
    $resetSql = @"
BEGIN;

-- Delete leases tied to seeded tenants
DELETE FROM public.leases
WHERE tenant_id IN (
    SELECT id FROM public.tenants WHERE email LIKE '%@seed.urbano'
);

-- Delete tenants
DELETE FROM public.tenants
WHERE email LIKE '%@seed.urbano';

-- Delete unit photo urls for seeded units
DELETE FROM public.unit_photo_urls
WHERE unit_id IN (
    SELECT u.id FROM public.units u
    JOIN public.properties p ON p.id = u.property_id
    WHERE p.pm_account_id IN (
        SELECT id FROM public.pm_accounts WHERE company_name LIKE 'Seed %'
    )
);

-- Delete units for seeded properties
DELETE FROM public.units
WHERE property_id IN (
    SELECT id FROM public.properties
    WHERE pm_account_id IN (
        SELECT id FROM public.pm_accounts WHERE company_name LIKE 'Seed %'
    )
);

-- Delete properties
DELETE FROM public.properties
WHERE pm_account_id IN (
    SELECT id FROM public.pm_accounts WHERE company_name LIKE 'Seed %'
);

-- Delete users (PM_ADMINs and TENANTs)
DELETE FROM public.auth_users
WHERE email LIKE '%@seed.urbano';

-- Delete pm_accounts
DELETE FROM public.pm_accounts
WHERE company_name LIKE 'Seed %';

COMMIT;
"@

    Invoke-Psql -Sql $resetSql | Out-Null
    Write-Host "✅ Reset complete." -ForegroundColor Green
}

# ============================================================
# 5. The main seed — one transaction, deterministic UUIDs
# ------------------------------------------------------------
# We use gen_random_uuid() and CTEs so we don't have to track
# IDs in PowerShell. All inserts run inside a single transaction.
# ============================================================

# Landlord A:    pmA / user pmA / 2 properties / 6 units
# Landlord B:    pmB / user pmB / 2 properties / 6 units
# Tenants:       tenantA1, tenantA2 (under Landlord A), tenantB1 (under Landlord B)
# Leases:        1 active lease per tenant

$seedSql = @"
BEGIN;

-- ------------------------------------------------------------------
-- 1. PM accounts (the two landlords)
-- ------------------------------------------------------------------
INSERT INTO public.pm_accounts (id, company_name, service_option, is_active, approval_status, created_at)
VALUES
    ('aaaaaaaa-0000-0000-0000-000000000001', 'Seed Landlord A Ltd', 'SOFTWARE_ONLY', true, 'ACTIVE', NOW()),
    ('aaaaaaaa-0000-0000-0000-000000000002', 'Seed Landlord B Ltd', 'SOFTWARE_ONLY', true, 'ACTIVE', NOW());

-- ------------------------------------------------------------------
-- 2. Landlord users (PM_ADMIN)
-- ------------------------------------------------------------------
INSERT INTO public.auth_users (
    id, email, password_hash, first_name, last_name, phone, role, status,
    pm_account_id, phone_verified, email_verified, is_active, created_at, updated_at
)
VALUES
    ('bbbbbbbb-0000-0000-0000-000000000001',
     'landlord.a@seed.urbano', '$bcryptHash',
     'Alice', 'Wanjiru', '+254700100001', 'PM_ADMIN', 'ACTIVE',
     'aaaaaaaa-0000-0000-0000-000000000001', true, true, true, NOW(), NOW()),

    ('bbbbbbbb-0000-0000-0000-000000000002',
     'landlord.b@seed.urbano', '$bcryptHash',
     'Brian', 'Otieno', '+254700100002', 'PM_ADMIN', 'ACTIVE',
     'aaaaaaaa-0000-0000-0000-000000000002', true, true, true, NOW(), NOW());

-- ------------------------------------------------------------------
-- 3. Tenant users (TENANT role, linked to their landlord's pm_account_id)
-- ------------------------------------------------------------------
INSERT INTO public.auth_users (
    id, email, password_hash, first_name, last_name, phone, role, status,
    pm_account_id, phone_verified, email_verified, is_active, created_at, updated_at
)
VALUES
    ('cccccccc-0000-0000-0000-000000000001',
     'tenant.a1@seed.urbano', '$bcryptHash',
     'Carol', 'Njeri', '+254700200001', 'TENANT', 'ACTIVE',
     'aaaaaaaa-0000-0000-0000-000000000001', true, true, true, NOW(), NOW()),

    ('cccccccc-0000-0000-0000-000000000002',
     'tenant.a2@seed.urbano', '$bcryptHash',
     'David', 'Kimani', '+254700200002', 'TENANT', 'ACTIVE',
     'aaaaaaaa-0000-0000-0000-000000000001', true, true, true, NOW(), NOW()),

    ('cccccccc-0000-0000-0000-000000000003',
     'tenant.b1@seed.urbano', '$bcryptHash',
     'Eve', 'Achieng', '+254700200003', 'TENANT', 'ACTIVE',
     'aaaaaaaa-0000-0000-0000-000000000002', true, true, true, NOW(), NOW());

-- ------------------------------------------------------------------
-- 4. Tenant records (business table, distinct from auth_users)
-- ------------------------------------------------------------------
INSERT INTO public.tenants (
    id, pm_account_id, user_id, first_name, last_name, full_name, email, phone,
    is_active, credit_balance, invite_status, activated_at, created_at, updated_at
)
VALUES
    ('dddddddd-0000-0000-0000-000000000001',
     'aaaaaaaa-0000-0000-0000-000000000001',
     'cccccccc-0000-0000-0000-000000000001',
     'Carol', 'Njeri', 'Carol Njeri', 'tenant.a1@seed.urbano', '+254700200001',
     true, 0.0, 'ACTIVATED', NOW(), NOW(), NOW()),

    ('dddddddd-0000-0000-0000-000000000002',
     'aaaaaaaa-0000-0000-0000-000000000001',
     'cccccccc-0000-0000-0000-000000000002',
     'David', 'Kimani', 'David Kimani', 'tenant.a2@seed.urbano', '+254700200002',
     true, 0.0, 'ACTIVATED', NOW(), NOW(), NOW()),

    ('dddddddd-0000-0000-0000-000000000003',
     'aaaaaaaa-0000-0000-0000-000000000002',
     'cccccccc-0000-0000-0000-000000000003',
     'Eve', 'Achieng', 'Eve Achieng', 'tenant.b1@seed.urbano', '+254700200003',
     true, 0.0, 'ACTIVATED', NOW(), NOW(), NOW());

-- ------------------------------------------------------------------
-- 5. Properties (2 per landlord, APPROVED so they're public)
-- ------------------------------------------------------------------
INSERT INTO public.properties (
    id, name, description, address, city, state, country, zip_code,
    type, total_units, status, owner_id, owner_name, owner_email,
    amenities, latitude, longitude, location, pm_account_id,
    approval_status, approved_at, created_at, updated_at
)
VALUES
    ('eeeeeeee-0000-0000-0000-000000000001',
     'Kilimani Heights', 'Modern apartment block in Kilimani',
     'Ring Road, Kilimani', 'Nairobi', 'Nairobi', 'Kenya', '00100',
     'RESIDENTIAL', 3, 'AVAILABLE',
     'bbbbbbbb-0000-0000-0000-000000000001', 'Alice Wanjiru', 'landlord.a@seed.urbano',
     'Elevator, Parking, Security, Backup Generator', -1.2921, 36.8219, 'Kilimani',
     'aaaaaaaa-0000-0000-0000-000000000001', 'APPROVED', NOW(), NOW(), NOW()),

    ('eeeeeeee-0000-0000-0000-000000000002',
     'Westlands Suites', 'Serviced apartments near Sarit Centre',
     'Mpaka Road, Westlands', 'Nairobi', 'Nairobi', 'Kenya', '00800',
     'RESIDENTIAL', 3, 'AVAILABLE',
     'bbbbbbbb-0000-0000-0000-000000000001', 'Alice Wanjiru', 'landlord.a@seed.urbano',
     'Gym, Swimming Pool, 24/7 Security', -1.2635, 36.8032, 'Westlands',
     'aaaaaaaa-0000-0000-0000-000000000001', 'APPROVED', NOW(), NOW(), NOW()),

    ('eeeeeeee-0000-0000-0000-000000000003',
     'Karen Villas', 'Gated community villas in Karen',
     'Karen Road, Karen', 'Nairobi', 'Nairobi', 'Kenya', '00502',
     'RESIDENTIAL', 3, 'AVAILABLE',
     'bbbbbbbb-0000-0000-0000-000000000002', 'Brian Otieno', 'landlord.b@seed.urbano',
     'Garden, Parking, Security', -1.3197, 36.7072, 'Karen',
     'aaaaaaaa-0000-0000-0000-000000000002', 'APPROVED', NOW(), NOW(), NOW()),

    ('eeeeeeee-0000-0000-0000-000000000004',
     'Lavington Court', 'Quiet apartments in Lavington',
     'James Gichuru Road, Lavington', 'Nairobi', 'Nairobi', 'Kenya', '00603',
     'RESIDENTIAL', 3, 'AVAILABLE',
     'bbbbbbbb-0000-0000-0000-000000000002', 'Brian Otieno', 'landlord.b@seed.urbano',
     'Parking, Borehole Water, Security', -1.2792, 36.7684, 'Lavington',
     'aaaaaaaa-0000-0000-0000-000000000002', 'APPROVED', NOW(), NOW(), NOW());

-- ------------------------------------------------------------------
-- 6. Units — 3 per property, 12 total
-- ------------------------------------------------------------------
INSERT INTO public.units (
    id, property_id, unit_number, floor, square_footage, bedrooms, bathrooms,
    rent_amount, currency, is_available, status, property_type, transaction_type,
    description, features, label, published, created_at, updated_at
)
VALUES
    -- Kilimani Heights (landlord A)
    ('ffffffff-0000-0000-0000-000000000001', 'eeeeeeee-0000-0000-0000-000000000001',
     'A101', 1, 85.0, 2, 2, 55000.0, 'KES', false, 'OCCUPIED', 'APARTMENT', 'FOR_RENT',
     'Corner unit with balcony', 'Balcony, Ensuite', 'Block A', true, NOW(), NOW()),
    ('ffffffff-0000-0000-0000-000000000002', 'eeeeeeee-0000-0000-0000-000000000001',
     'A102', 1, 78.0, 2, 1, 50000.0, 'KES', true, 'AVAILABLE', 'APARTMENT', 'FOR_RENT',
     'Quiet inner unit', 'Ensuite', 'Block A', true, NOW(), NOW()),
    ('ffffffff-0000-0000-0000-000000000003', 'eeeeeeee-0000-0000-0000-000000000001',
     'A201', 2, 92.0, 3, 2, 65000.0, 'KES', true, 'AVAILABLE', 'APARTMENT', 'FOR_RENT',
     'Top floor with view', 'Balcony, Ensuite, Store', 'Block A', true, NOW(), NOW()),

    -- Westlands Suites (landlord A)
    ('ffffffff-0000-0000-0000-000000000004', 'eeeeeeee-0000-0000-0000-000000000002',
     'W301', 3, 70.0, 1, 1, 45000.0, 'KES', true, 'AVAILABLE', 'STUDIO', 'FOR_RENT',
     'Furnished studio', 'Furnished, AC', 'Tower 1', true, NOW(), NOW()),
    ('ffffffff-0000-0000-0000-000000000005', 'eeeeeeee-0000-0000-0000-000000000002',
     'W302', 3, 105.0, 2, 2, 70000.0, 'KES', true, 'AVAILABLE', 'APARTMENT', 'FOR_RENT',
     'Executive apartment', 'Furnished, AC, Balcony', 'Tower 1', true, NOW(), NOW()),
    ('ffffffff-0000-0000-0000-000000000006', 'eeeeeeee-0000-0000-0000-000000000002',
     'W401', 4, 140.0, 3, 3, 95000.0, 'KES', true, 'AVAILABLE', 'PENTHOUSE', 'FOR_RENT',
     'Penthouse with rooftop terrace', 'Terrace, Ensuite, Store', 'Tower 1', true, NOW(), NOW()),

    -- Karen Villas (landlord B)
    ('ffffffff-0000-0000-0000-000000000007', 'eeeeeeee-0000-0000-0000-000000000003',
     'K1', 1, 220.0, 4, 3, 150000.0, 'KES', false, 'OCCUPIED', 'VILLA', 'FOR_RENT',
     '4-bed villa with garden', 'Garden, DSQ, Parking', 'Block K', true, NOW(), NOW()),
    ('ffffffff-0000-0000-0000-000000000008', 'eeeeeeee-0000-0000-0000-000000000003',
     'K2', 1, 240.0, 4, 4, 165000.0, 'KES', true, 'AVAILABLE', 'VILLA', 'FOR_RENT',
     'Family villa with large garden', 'Garden, DSQ, Parking', 'Block K', true, NOW(), NOW()),
    ('ffffffff-0000-0000-0000-000000000009', 'eeeeeeee-0000-0000-0000-000000000003',
     'K3', 1, 260.0, 5, 4, 185000.0, 'KES', true, 'AVAILABLE', 'VILLA', 'FOR_RENT',
     'Premium villa', 'Garden, DSQ, Parking, Pool', 'Block K', true, NOW(), NOW()),

    -- Lavington Court (landlord B)
    ('ffffffff-0000-0000-0000-00000000000a', 'eeeeeeee-0000-0000-0000-000000000004',
     'L101', 1, 88.0, 2, 2, 60000.0, 'KES', true, 'AVAILABLE', 'APARTMENT', 'FOR_RENT',
     'Ground floor with garden access', 'Garden, Ensuite', 'Block L', true, NOW(), NOW()),
    ('ffffffff-0000-0000-0000-00000000000b', 'eeeeeeee-0000-0000-0000-000000000004',
     'L102', 1, 95.0, 2, 2, 65000.0, 'KES', true, 'AVAILABLE', 'APARTMENT', 'FOR_RENT',
     'Spacious 2-bedroom', 'Ensuite, Store', 'Block L', true, NOW(), NOW()),
    ('ffffffff-0000-0000-0000-00000000000c', 'eeeeeeee-0000-0000-0000-000000000004',
     'L201', 2, 110.0, 3, 2, 80000.0, 'KES', true, 'AVAILABLE', 'APARTMENT', 'FOR_RENT',
     'Top floor 3-bedroom', 'Balcony, Ensuite', 'Block L', true, NOW(), NOW());

-- ------------------------------------------------------------------
-- 7. Update units.current_tenant_id for the two occupied units
-- ------------------------------------------------------------------
UPDATE public.units
SET current_tenant_id = 'dddddddd-0000-0000-0000-000000000001',
    rented_at         = NOW()
WHERE id = 'ffffffff-0000-0000-0000-000000000001';

UPDATE public.units
SET current_tenant_id = 'dddddddd-0000-0000-0000-000000000003',
    rented_at         = NOW()
WHERE id = 'ffffffff-0000-0000-0000-000000000007';

-- ------------------------------------------------------------------
-- 8. Leases — 1 active per tenant
--    tenant.a1 -> unit A101 (Kilimani)
--    tenant.a2 -> unit A102 (Kilimani)
--    tenant.b1 -> unit K1   (Karen)
-- ------------------------------------------------------------------
INSERT INTO public.leases (
    id, pm_account_id, tenant_id, unit_id, property_id,
    start_date, end_date, rent_amount, security_deposit,
    payment_frequency, terms, currency, status, is_active,
    signed_at, created_at, updated_at
)
VALUES
    ('11111111-0000-0000-0000-000000000001',
     'aaaaaaaa-0000-0000-0000-000000000001',
     'dddddddd-0000-0000-0000-000000000001',
     'ffffffff-0000-0000-0000-000000000001',
     'eeeeeeee-0000-0000-0000-000000000001',
     NOW() - INTERVAL '6 months', NOW() + INTERVAL '6 months',
     55000.00, 55000.00, 'MONTHLY',
     'Standard 12-month lease. Rent due on the 5th of each month.',
     'KES', 'ACTIVE', true, NOW() - INTERVAL '6 months', NOW(), NOW()),

    ('11111111-0000-0000-0000-000000000002',
     'aaaaaaaa-0000-0000-0000-000000000001',
     'dddddddd-0000-0000-0000-000000000002',
     'ffffffff-0000-0000-0000-000000000002',
     'eeeeeeee-0000-0000-0000-000000000001',
     NOW() - INTERVAL '3 months', NOW() + INTERVAL '9 months',
     50000.00, 50000.00, 'MONTHLY',
     'Standard 12-month lease. Rent due on the 5th of each month.',
     'KES', 'ACTIVE', true, NOW() - INTERVAL '3 months', NOW(), NOW()),

    ('11111111-0000-0000-0000-000000000003',
     'aaaaaaaa-0000-0000-0000-000000000002',
     'dddddddd-0000-0000-0000-000000000003',
     'ffffffff-0000-0000-0000-000000000007',
     'eeeeeeee-0000-0000-0000-000000000003',
     NOW() - INTERVAL '2 months', NOW() + INTERVAL '10 months',
     150000.00, 150000.00, 'MONTHLY',
     'Standard 12-month lease. Rent due on the 5th of each month.',
     'KES', 'ACTIVE', true, NOW() - INTERVAL '2 months', NOW(), NOW());

COMMIT;
"@

Write-Host "🌱 Seeding the database..." -ForegroundColor Cyan
Invoke-Psql -Sql $seedSql | Out-Null
Write-Host "✅ Seed complete." -ForegroundColor Green

# ============================================================
# 6. Summary
# ============================================================
$summary = @"
SELECT 'pm_accounts' AS table_name, COUNT(*)::text AS rows FROM public.pm_accounts WHERE company_name LIKE 'Seed %'
UNION ALL SELECT 'auth_users (PM_ADMIN + TENANT)', COUNT(*)::text FROM public.auth_users WHERE email LIKE '%@seed.urbano'
UNION ALL SELECT 'tenants',         COUNT(*)::text FROM public.tenants WHERE email LIKE '%@seed.urbano'
UNION ALL SELECT 'properties',      COUNT(*)::text FROM public.properties WHERE pm_account_id IN (SELECT id FROM public.pm_accounts WHERE company_name LIKE 'Seed %')
UNION ALL SELECT 'units',           COUNT(*)::text FROM public.units WHERE property_id IN (SELECT id FROM public.properties WHERE pm_account_id IN (SELECT id FROM public.pm_accounts WHERE company_name LIKE 'Seed %'))
UNION ALL SELECT 'leases',          COUNT(*)::text FROM public.leases WHERE pm_account_id IN (SELECT id FROM public.pm_accounts WHERE company_name LIKE 'Seed %');
"@

Write-Host "`n📊 Summary:" -ForegroundColor Cyan
Invoke-Psql -Sql $summary | Format-Table -AutoSize

Write-Host "`n🔑 Login credentials (all users):" -ForegroundColor Cyan
@"
  landlord.a@seed.urbano   / Password123!
  landlord.b@seed.urbano   / Password123!
  tenant.a1@seed.urbano    / Password123!
  tenant.a2@seed.urbano    / Password123!
  tenant.b1@seed.urbano    / Password123!
"@ | Write-Host

Write-Host "`n🏠 Ownership map:" -ForegroundColor Cyan
@"
  Landlord A (landlord.a@seed.urbano)
    ├── Kilimani Heights  → A101 (leased to tenant.a1), A102 (leased to tenant.a2), A201
    └── Westlands Suites  → W301, W302, W401

  Landlord B (landlord.b@seed.urbano)
    ├── Karen Villas      → K1 (leased to tenant.b1), K2, K3
    └── Lavington Court   → L101, L102, L201
"@ | Write-Host

$env:PGPASSWORD = $null