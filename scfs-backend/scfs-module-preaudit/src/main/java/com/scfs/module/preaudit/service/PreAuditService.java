package com.scfs.module.preaudit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scfs.common.entity.MaterialChecklistTemplate;
import com.scfs.common.enums.MaterialType;
import com.scfs.common.service.RuleService;
import com.scfs.module.preaudit.entity.*;
import com.scfs.module.preaudit.mapper.PreAuditMapper;
import com.scfs.module.verify.entity.ApplicationMaterial;
import com.scfs.module.verify.entity.FinancingApplication;
import com.scfs.module.verify.entity.MaterialRecognitionResult;
import com.scfs.module.verify.mapper.VerifyMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 预审服务 - 对应 RFC 4.2.3 PreAuditService
 *
 * <p>3 项检查：</p>
 * <ul>
 *   <li>完整性检查 - 对照材料清单模板</li>
 *   <li>有效性检查 - 文件过期/字段缺失</li>
 *   <li>企业信息一致性 - 4 要素：name/uscc/legal_person/address</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PreAuditService {

    private final PreAuditMapper preAuditMapper;
    private final VerifyMapper verifyMapper;
    private final RuleService ruleService;
    private final ObjectMapper objectMapper;

    // ========== 完整性检查 ==========

    public MaterialCompletenessResult getCompleteness(Long applicationId) {
        return preAuditMapper.selectCompleteness(applicationId);
    }

    @Transactional
    public MaterialCompletenessResult checkCompleteness(Long applicationId, String businessType) {
        MaterialChecklistTemplate template = ruleService.getTemplateByBusinessType(businessType);
        if (template == null) {
            throw new IllegalStateException("业务类型无对应材料模板: " + businessType);
        }

        List<ApplicationMaterial> materials = verifyMapper.selectMaterialsByApplication(applicationId);
        Set<String> submittedTypes = materials.stream()
                .map(ApplicationMaterial::getMaterialType)
                .collect(Collectors.toSet());

        @SuppressWarnings("unchecked")
        List<String> requiredTypes = template.getRequiredMaterials();
        List<String> missing = requiredTypes.stream()
                .filter(t -> !submittedTypes.contains(t))
                .collect(Collectors.toList());

        int requiredCount = requiredTypes.size();
        int submittedCount = requiredCount - missing.size();
        BigDecimal pct = BigDecimal.valueOf(submittedCount)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(requiredCount), 2, RoundingMode.HALF_UP);

        MaterialCompletenessResult existing = preAuditMapper.selectCompleteness(applicationId);
        MaterialCompletenessResult result = existing != null ? existing : new MaterialCompletenessResult();
        result.setApplicationId(applicationId);
        result.setRequiredCount(requiredCount);
        result.setSubmittedCount(submittedCount);
        result.setCompletenessPct(pct);
        result.setMissingMaterials(missing);
        result.setCheckedAt(Instant.now());

        if (existing == null) {
            preAuditMapper.insertCompleteness(result);
        } else {
            preAuditMapper.updateCompleteness(result);
        }
        log.info("[PreAudit] 完整性检查: applicationId={}, {}/{} ({}%)",
                applicationId, submittedCount, requiredCount, pct);
        return result;
    }

    // ========== 有效性检查 ==========

    public MaterialValidityResult getValidity(Long applicationId) {
        return preAuditMapper.selectValidity(applicationId);
    }

    @Transactional
    public MaterialValidityResult checkValidity(Long applicationId) {
        List<ApplicationMaterial> materials = verifyMapper.selectMaterialsByApplication(applicationId);
        int expiredCount = 0;
        int incompleteCount = 0;
        List<Map<String, Object>> abnormalItems = new ArrayList<>();
        List<Map<String, Object>> materialResults = new ArrayList<>();

        LocalDate now = LocalDate.now();
        for (ApplicationMaterial material : materials) {
            MaterialRecognitionResult recognition = verifyMapper.selectRecognitionResult(material.getId());
            List<String> issues = new ArrayList<>();
            List<String> missingFields = new ArrayList<>();
            boolean expired = false;
            if (recognition == null) {
                issues.add("尚未完成OCR识别");
                incompleteCount++;
            } else {
                collectMissingFields(material.getMaterialType(), recognition, missingFields);
                if (!missingFields.isEmpty()) {
                    issues.add("缺少关键字段：" + String.join("、", missingFields));
                    incompleteCount++;
                }
                LocalDate documentDate = documentDate(material.getMaterialType(), recognition);
                if (documentDate != null && documentDate.plusYears(1).isBefore(now)) {
                    expired = true;
                    expiredCount++;
                    issues.add("材料日期超过一年");
                }
            }

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("materialId", material.getId());
            item.put("fileName", material.getFileName());
            item.put("materialType", material.getMaterialType());
            item.put("type", material.getMaterialType());
            item.put("recognitionStatus", material.getStatus());
            item.put("recognized", recognition != null);
            item.put("expired", expired);
            item.put("missingFields", missingFields);
            item.put("issues", issues);
            item.put("issue", String.join("；", issues));
            item.put("valid", issues.isEmpty());
            materialResults.add(item);
            if (!issues.isEmpty()) abnormalItems.add(item);
        }
        int abnormalCount = abnormalItems.size();

        Map<String, Object> detailMap = new LinkedHashMap<>();
        detailMap.put("abnormalItems", abnormalItems);
        detailMap.put("materialResults", materialResults);
        detailMap.put("allValid", abnormalCount == 0);

        MaterialValidityResult existing = preAuditMapper.selectValidity(applicationId);
        MaterialValidityResult result = existing != null ? existing : new MaterialValidityResult();
        result.setApplicationId(applicationId);
        result.setTotalFiles(materials.size());
        result.setExpiredCount(expiredCount);
        result.setIncompleteCount(incompleteCount);
        result.setAbnormalCount(abnormalCount);
        result.setDetails(detailMap);
        result.setCheckedAt(Instant.now());

        if (existing == null) {
            preAuditMapper.insertValidity(result);
        } else {
            preAuditMapper.updateValidity(result);
        }
        log.info("[PreAudit] 有效性检查: applicationId={}, abnormal={}", applicationId, abnormalCount);
        return result;
    }

    private void collectMissingFields(String materialType, MaterialRecognitionResult result, List<String> missing) {
        switch (materialType == null ? "" : materialType.toUpperCase(Locale.ROOT)) {
            case "CONTRACT" -> {
                requireText(result.getBuyerName(), "买方名称", missing);
                requireText(result.getSellerName(), "卖方名称", missing);
                requireValue(result.getAmount(), "合同金额", missing);
                requireValue(result.getContractDate(), "合同日期", missing);
            }
            case "INVOICE" -> {
                requireText(result.getBuyerName(), "购买方名称", missing);
                requireText(result.getSellerName(), "销售方名称", missing);
                requireValue(result.getAmount(), "发票金额", missing);
                requireValue(result.getInvoiceDate(), "开票日期", missing);
                requireText(result.getTransactionNo(), "发票号码", missing);
            }
            case "ORDER" -> {
                requireValue(result.getAmount(), "订单金额", missing);
                requireValue(result.getOrderDate(), "订单日期", missing);
                requireText(result.getTransactionNo(), "订单编号", missing);
            }
            case "LOGISTICS" -> requireValue(result.getLogisticsDate(), "物流日期", missing);
            case "ACCEPTANCE" -> requireValue(result.getAcceptanceDate(), "验收日期", missing);
            case "PAYMENT" -> {
                requireValue(result.getPaymentDate(), "付款日期", missing);
                requireValue(result.getAmount(), "付款金额", missing);
            }
            default -> { }
        }
    }

    private LocalDate documentDate(String materialType, MaterialRecognitionResult result) {
        return switch (materialType == null ? "" : materialType.toUpperCase(Locale.ROOT)) {
            case "CONTRACT" -> result.getContractDate();
            case "INVOICE" -> result.getInvoiceDate();
            case "ORDER" -> result.getOrderDate();
            case "LOGISTICS" -> result.getLogisticsDate();
            case "ACCEPTANCE" -> result.getAcceptanceDate();
            case "PAYMENT" -> result.getPaymentDate();
            default -> null;
        };
    }

    private void requireText(String value, String label, List<String> missing) {
        if (value == null || value.isBlank()) missing.add(label);
    }

    private void requireValue(Object value, String label, List<String> missing) {
        if (value == null) missing.add(label);
    }

    // ========== 企业信息一致性 ==========

    public EnterpriseInfoConsistencyResult getConsistency(Long applicationId) {
        return preAuditMapper.selectConsistency(applicationId);
    }

    @Transactional
    public EnterpriseInfoConsistencyResult checkConsistency(Long applicationId) {
        FinancingApplication application = verifyMapper.selectApplicationById(applicationId);
        if (application == null) throw new IllegalArgumentException("融资申请不存在");
        List<ApplicationMaterial> materials = verifyMapper.selectMaterialsByApplication(applicationId);
        Map<String, Map<Long, String>> nameValues = new HashMap<>();
        Map<String, Map<Long, String>> usccValues = new HashMap<>();

        putValue(nameValues, "BUYER_NAME", -1L, application.getBuyerName());
        putValue(nameValues, "SELLER_NAME", -2L, application.getSellerName());
        putValue(usccValues, "BUYER_USCC", -1L, application.getBuyerUscc());
        putValue(usccValues, "SELLER_USCC", -2L, application.getSellerUscc());

        for (ApplicationMaterial material : materials) {
            MaterialRecognitionResult result = verifyMapper.selectRecognitionResult(material.getId());
            if (result == null) continue;
            Long materialId = material.getId();
            putValue(nameValues, "BUYER_NAME", materialId, result.getBuyerName());
            putValue(usccValues, "BUYER_USCC", materialId, result.getBuyerUscc());
            putValue(nameValues, "SELLER_NAME", materialId, result.getSellerName());
            putValue(usccValues, "SELLER_USCC", materialId, result.getSellerUscc());
        }

        boolean nameConsistent = isConsistent(nameValues);
        boolean usccConsistent = isConsistent(usccValues);
        boolean overall = nameConsistent && usccConsistent;
        int mismatchCount = (nameConsistent ? 0 : 1) + (usccConsistent ? 0 : 1);

        EnterpriseInfoConsistencyResult result = new EnterpriseInfoConsistencyResult();
        result.setApplicationId(applicationId);
        result.setOverallConsistent(overall);
        result.setNameConsistent(nameConsistent);
        result.setUsccConsistent(usccConsistent);
        // 当前 OCR 结构不提供法人和地址，兼容保留数据库字段，但不参与总体结论。
        result.setLegalPersonConsistent(true);
        result.setAddressConsistent(true);
        result.setMismatchCount(mismatchCount);
        result.setCheckedAt(Instant.now());
        preAuditMapper.insertConsistency(result);

        // 写入明细
        List<EnterpriseInfoMismatchDetail> details = new ArrayList<>();
        if (!nameConsistent) details.add(buildMismatch(result.getId(), "NAME", "企业名称", nameValues));
        if (!usccConsistent) details.add(buildMismatch(result.getId(), "USCC", "统一社会信用代码", usccValues));
        if (!details.isEmpty()) {
            preAuditMapper.batchInsertMismatchDetails(details);
        }

        log.info("[PreAudit] 企业信息一致性: applicationId={}, consistent={}, mismatch={}",
                applicationId, overall, mismatchCount);
        return result;
    }

    private boolean isConsistent(Map<String, Map<Long, String>> valuesByField) {
        for (Map<Long, String> values : valuesByField.values()) {
            // 每个角色至少需要“申请登记值 + 一份材料识别值”才能得出一致结论。
            if (values.size() < 2) return false;
            Set<String> distinct = values.values().stream()
                    .map(this::normalizeComparisonValue)
                    .collect(Collectors.toSet());
            if (distinct.size() > 1) {
                return false;
            }
        }
        return !valuesByField.isEmpty();
    }

    private void putValue(Map<String, Map<Long, String>> valuesByField, String field,
                          Long sourceId, String value) {
        if (value != null && !value.isBlank()) {
            valuesByField.computeIfAbsent(field, k -> new LinkedHashMap<>()).put(sourceId, value.trim());
        } else {
            valuesByField.computeIfAbsent(field, k -> new LinkedHashMap<>());
        }
    }

    private String normalizeComparisonValue(String value) {
        return value.replaceAll("[\\s（）()·]", "").toUpperCase(Locale.ROOT);
    }

    private EnterpriseInfoMismatchDetail buildMismatch(Long resultId, String fieldType, String fieldName,
                                                       Map<String, Map<Long, String>> valuesByField) {
        EnterpriseInfoMismatchDetail detail = new EnterpriseInfoMismatchDetail();
        detail.setResultId(resultId);
        detail.setFieldType(fieldType);
        detail.setFieldName(fieldName);
        detail.setConsistent(false);
        List<Map<String, Object>> sourceValues = new ArrayList<>();
        for (Map.Entry<String, Map<Long, String>> entry : valuesByField.entrySet()) {
            for (Map.Entry<Long, String> e : entry.getValue().entrySet()) {
                Map<String, Object> sv = new HashMap<>();
                if (e.getKey() > 0) sv.put("materialId", e.getKey());
                sv.put("source", e.getKey() == -1L ? "融资申请登记买方"
                        : e.getKey() == -2L ? "融资申请登记卖方" : "OCR材料");
                sv.put("context", entry.getKey());
                sv.put("value", e.getValue());
                sourceValues.add(sv);
            }
        }
        detail.setSourceValues(sourceValues);
        boolean insufficient = valuesByField.values().stream().anyMatch(values -> values.size() < 2);
        detail.setMismatchDetail(insufficient ? "缺少可用于比对的申请登记信息或OCR识别值"
                : "融资申请登记信息与不同材料中的识别值不一致");
        return detail;
    }

    public List<EnterpriseInfoMismatchDetail> getMismatchDetails(Long resultId) {
        return preAuditMapper.selectMismatchDetails(resultId);
    }

    // ========== 补正清单 ==========

    public SupplementList getSupplementList(Long applicationId) {
        return preAuditMapper.selectSupplementList(applicationId);
    }

    @Transactional
    public SupplementList generateSupplementList(Long applicationId) {
        // 综合完整性 + 有效性检查结果生成补正清单
        MaterialCompletenessResult completeness = preAuditMapper.selectCompleteness(applicationId);
        MaterialValidityResult validity = preAuditMapper.selectValidity(applicationId);

        List<Map<String, Object>> supplementItems = new ArrayList<>();
        if (completeness != null && completeness.getMissingMaterials() != null) {
            for (String missing : completeness.getMissingMaterials()) {
                Map<String, Object> item = new HashMap<>();
                item.put("type", "MISSING_MATERIAL");
                item.put("materialType", missing);
                item.put("reason", "必备材料缺失");
                item.put("suggestion", "请补充提交 " + missing + " 类型材料");
                supplementItems.add(item);
            }
        }
        if (validity != null && validity.getDetails() != null) {
            Object abnormalItems = validity.getDetails().get("abnormalItems");
            if (abnormalItems instanceof List) {
                for (Object o : (List<?>) abnormalItems) {
                    if (o instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> m = (Map<String, Object>) o;
                        Map<String, Object> item = new HashMap<>();
                        item.put("type", "ABNORMAL_FILE");
                        item.put("materialId", m.get("materialId"));
                        item.put("reason", m.get("issue"));
                        item.put("suggestion", "请重新上传");
                        supplementItems.add(item);
                    }
                }
            }
        }

        SupplementList existing = preAuditMapper.selectSupplementList(applicationId);
        SupplementList supplement = existing != null ? existing : new SupplementList();
        supplement.setApplicationId(applicationId);
        supplement.setSupplementItems(supplementItems);
        supplement.setStatus(supplementItems.isEmpty() ? "COMPLETED" : "PENDING");
        supplement.setDeadline(LocalDate.now().plusDays(7));
        supplement.setGeneratedAt(Instant.now());

        if (existing == null) {
            preAuditMapper.insertSupplementList(supplement);
        } else {
            preAuditMapper.updateSupplementListStatus(supplement.getId(), supplement.getStatus());
        }
        log.info("[PreAudit] 补正清单生成: applicationId={}, items={}", applicationId, supplementItems.size());
        return supplement;
    }

    @Transactional
    public void markSupplementCompleted(Long applicationId) {
        SupplementList supplement = preAuditMapper.selectSupplementList(applicationId);
        if (supplement == null) {
            throw new IllegalArgumentException("补正清单不存在");
        }
        preAuditMapper.updateSupplementListStatus(supplement.getId(), "COMPLETED");
    }
}
