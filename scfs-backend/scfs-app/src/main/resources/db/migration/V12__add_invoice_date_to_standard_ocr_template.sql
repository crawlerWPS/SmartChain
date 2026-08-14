UPDATE schema_verify.ocr_recognition_template
SET field_rules = field_rules ||
    '[{"fieldCode":"invoiceDate","extractMode":"FULL_TEXT","pattern":"(?:开票日期|开具日期|发票日期)[：\\s]*(\\d{4}[年./-]\\d{1,2}[月./-]\\d{1,2}日?)","page":1,"required":true}]'::jsonb,
    updated_at = NOW()
WHERE material_type = 'INVOICE'
  AND template_name = '标准增值税发票'
  AND NOT EXISTS (
      SELECT 1 FROM jsonb_array_elements(field_rules) rule
      WHERE rule ->> 'fieldCode' = 'invoiceDate'
  );
