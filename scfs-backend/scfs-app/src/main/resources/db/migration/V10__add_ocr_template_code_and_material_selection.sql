ALTER TABLE schema_verify.ocr_recognition_template
    ADD COLUMN IF NOT EXISTS template_code VARCHAR(64);

UPDATE schema_verify.ocr_recognition_template
SET template_code = 'OCR_' || material_type || '_' || LPAD(id::text, 4, '0')
WHERE template_code IS NULL OR BTRIM(template_code) = '';

ALTER TABLE schema_verify.ocr_recognition_template
    ALTER COLUMN template_code SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_ocr_template_code
    ON schema_verify.ocr_recognition_template(template_code);

ALTER TABLE schema_verify.application_material
    ADD COLUMN IF NOT EXISTS ocr_template_id BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_application_material_ocr_template'
    ) THEN
        ALTER TABLE schema_verify.application_material
            ADD CONSTRAINT fk_application_material_ocr_template
            FOREIGN KEY (ocr_template_id)
            REFERENCES schema_verify.ocr_recognition_template(id)
            ON DELETE SET NULL;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_application_material_ocr_template
    ON schema_verify.application_material(ocr_template_id);
