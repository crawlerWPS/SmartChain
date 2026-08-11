-- enterprise.id 作为客户号，融资申请直接关联买卖方企业。
ALTER TABLE schema_verify.financing_application
    ADD COLUMN IF NOT EXISTS buyer_enterprise_id BIGINT,
    ADD COLUMN IF NOT EXISTS seller_enterprise_id BIGINT;

-- 历史申请：融资企业作为卖方，直接交易对手作为买方。
UPDATE schema_verify.financing_application a
SET seller_enterprise_id = a.enterprise_id,
    buyer_enterprise_id = COALESCE(
        (SELECT CASE
            WHEN r.from_enterprise_id = a.enterprise_id THEN r.to_enterprise_id
            ELSE r.from_enterprise_id
         END
         FROM schema_graph.supply_chain_relation r
         WHERE r.from_enterprise_id = a.enterprise_id OR r.to_enterprise_id = a.enterprise_id
         ORDER BY r.level, r.id
         LIMIT 1),
        a.enterprise_id)
WHERE a.buyer_enterprise_id IS NULL OR a.seller_enterprise_id IS NULL;

ALTER TABLE schema_verify.financing_application
    DROP CONSTRAINT IF EXISTS fk_application_buyer_enterprise,
    DROP CONSTRAINT IF EXISTS fk_application_seller_enterprise;

ALTER TABLE schema_verify.financing_application
    ALTER COLUMN buyer_enterprise_id SET NOT NULL,
    ALTER COLUMN seller_enterprise_id SET NOT NULL,
    ADD CONSTRAINT fk_application_buyer_enterprise FOREIGN KEY (buyer_enterprise_id)
        REFERENCES schema_graph.enterprise(id),
    ADD CONSTRAINT fk_application_seller_enterprise FOREIGN KEY (seller_enterprise_id)
        REFERENCES schema_graph.enterprise(id);

CREATE INDEX IF NOT EXISTS idx_app_buyer_enterprise
    ON schema_verify.financing_application(buyer_enterprise_id);
CREATE INDEX IF NOT EXISTS idx_app_seller_enterprise
    ON schema_verify.financing_application(seller_enterprise_id);
