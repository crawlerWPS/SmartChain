CREATE TABLE IF NOT EXISTS schema_verify.ocr_recognition_template (
    id BIGSERIAL PRIMARY KEY,
    template_name VARCHAR(128) NOT NULL,
    material_type VARCHAR(32) NOT NULL,
    enterprise_id BIGINT,
    priority INT NOT NULL DEFAULT 0,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    match_anchors JSONB NOT NULL DEFAULT '[]'::jsonb,
    field_rules JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_ocr_template_match
    ON schema_verify.ocr_recognition_template(material_type, enterprise_id, enabled, priority DESC);

INSERT INTO schema_verify.ocr_recognition_template
    (template_name, material_type, priority, match_anchors, field_rules)
SELECT '标准贸易合同', 'CONTRACT', 10, '["合同"]'::jsonb,
       '[
         {"fieldCode":"buyerName","extractMode":"ANCHOR_REGION","anchors":["甲方","买方","买受人","采购方"],"direction":"RIGHT","page":1,"region":{"x":0,"y":-0.02,"width":0.45,"height":0.06},"removeLabels":true,"required":true,"minConfidence":0.75},
         {"fieldCode":"sellerName","extractMode":"ANCHOR_REGION","anchors":["乙方","卖方","出卖人","供货方"],"direction":"RIGHT","page":1,"region":{"x":0,"y":-0.02,"width":0.45,"height":0.06},"removeLabels":true,"required":true,"minConfidence":0.75},
         {"fieldCode":"transactionNo","extractMode":"FULL_TEXT","pattern":"(?:合同编号|合同号)[：:\\s]*([A-Za-z0-9_-]{4,})","page":1},
         {"fieldCode":"amount","extractMode":"FULL_TEXT","pattern":"(?:合同金额|总金额|价税合计)[：:\\s￥¥]*([0-9,]+(?:\\.[0-9]{1,2})?)"}
       ]'::jsonb
WHERE NOT EXISTS (SELECT 1 FROM schema_verify.ocr_recognition_template WHERE template_name='标准贸易合同');

INSERT INTO schema_verify.ocr_recognition_template
    (template_name, material_type, priority, match_anchors, field_rules)
SELECT '标准增值税发票', 'INVOICE', 10, '["发票","购买方","销售方"]'::jsonb,
       '[
         {"fieldCode":"buyerName","extractMode":"ANCHOR_REGION","anchors":["购买方名称","购方名称","购买方"],"direction":"RIGHT","page":1,"region":{"x":0,"y":-0.02,"width":0.42,"height":0.06},"removeLabels":true,"required":true,"minConfidence":0.75},
         {"fieldCode":"sellerName","extractMode":"ANCHOR_REGION","anchors":["销售方名称","销方名称","销售方"],"direction":"RIGHT","page":1,"region":{"x":0,"y":-0.02,"width":0.42,"height":0.06},"removeLabels":true,"required":true,"minConfidence":0.75},
         {"fieldCode":"transactionNo","extractMode":"FULL_TEXT","pattern":"(?:发票号码|发票号)[：:\\s]*([A-Za-z0-9]{8,20})","page":1},
         {"fieldCode":"amount","extractMode":"FULL_TEXT","pattern":"(?:价税合计|小写)[：:\\s￥¥]*([0-9,]+(?:\\.[0-9]{1,2})?)","page":1}
       ]'::jsonb
WHERE NOT EXISTS (SELECT 1 FROM schema_verify.ocr_recognition_template WHERE template_name='标准增值税发票');
