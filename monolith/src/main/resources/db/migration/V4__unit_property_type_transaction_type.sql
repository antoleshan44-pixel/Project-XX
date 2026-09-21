

ALTER TABLE public.units
    ADD COLUMN property_type    character varying(32),
    ADD COLUMN transaction_type character varying(32);

UPDATE public.units
SET property_type    = 'APARTMENT',
    transaction_type = 'FOR_RENT'
WHERE property_type IS NULL;

ALTER TABLE public.units
    ALTER COLUMN property_type    SET NOT NULL,
ALTER COLUMN transaction_type SET NOT NULL;

ALTER TABLE public.units
    ADD CONSTRAINT units_property_type_check CHECK (
        property_type::text = ANY (
    (ARRAY['APARTMENT','HOUSE','VILLA','STUDIO','PENTHOUSE'])::text[]
    )
    ),
    ADD CONSTRAINT units_transaction_type_check CHECK (
    transaction_type::text = ANY (
    (ARRAY['FOR_SALE','FOR_RENT'])::text[]
    )
    );