package com.scfs.module.verify.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scfs.common.entity.RuleDefinition;
import com.scfs.common.enums.CheckType;
import com.scfs.common.enums.RuleCategory;
import com.scfs.common.service.RuleService;
import com.scfs.module.verify.entity.*;
import com.scfs.module.verify.mapper.VerifyMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

/**
 * 真实性核验服务 - 对应 RFC 4.2.2 VerifyService + Drools 规则引擎
 *
 * <p>4 类核验：</p>
 * <ul>
 *   <li>SUBJECT - 主体一致性（买卖方名称）</li>
 *   <li>AMOUNT - 金额一致性（合同/发票/验收）</li>
 *   <li>TIME - 时间逻辑（合同&lt;发票&lt;物流&lt;验收&lt;付款）</li>
 *   <li>REPEAT - 重复融资（同企业已审批通过的 AR 融资）</li>
 * </ul>
 *
 * <p>对应 RFC S2-7/S2-8：Drools 引擎 + 4 项核验</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VerifyService {

    private final VerifyMapper verifyMapper;
    private final RuleService ruleService;
    private final ObjectMapper objectMapper;

    /**
     * 执行 4 类核验
     */
    @Transactional
    public List<VerifyCheckResult> verifyAll(Long applicationId) {
        log.info("[Verify] 开始核验: applicationId={}", applicationId);
        List<VerifyCheckResult> results = new ArrayList<>();

        // 1. 主体一致性
        results.add(verifySubject(applicationId));
        // 2. 金额一致性
        results.add(verifyAmount(applicationId));
        // 3. 时间逻辑
        results.add(verifyTime(applicationId));
        // 4. 重复融资
        results.add(verifyRepeat(applicationId));

        log.info("[Verify] 核验完成: applicationId={}, 共 {} 项", applicationId, results.size());
        return results;
    }

    /**
     * SUBJECT 核验：买卖方名称跨材料一致性
     */
    private VerifyCheckResult verifySubject(Long applicationId) {
        List<ApplicationMaterial> materials = verifyMapper.selectMaterialsByApplication(applicationId);
        Set<String> buyerNames = new HashSet<>();
        Set<String> sellerNames = new HashSet<>();
        List<String> executedRules = new ArrayList<>();

        for (ApplicationMaterial material : materials) {
            MaterialRecognitionResult result = verifyMapper.selectRecognitionResult(material.getId());
            if (result != null) {
                if (result.getBuyerName() != null) buyerNames.add(result.getBuyerName());
                if (result.getSellerName() != null) sellerNames.add(result.getSellerName());
            }
        }

        boolean pass = buyerNames.size() <= 1 && sellerNames.size() <= 1;
        Map<String, Object> details = new HashMap<>();
        details.put("buyerNames", buyerNames);
        details.put("sellerNames", sellerNames);
        executedRules.add("R_SUBJECT_CONSISTENCY");

        VerifyCheckResult result = new VerifyCheckResult();
        result.setApplicationId(applicationId);
        result.setCheckType(CheckType.SUBJECT.name());
        result.setResult(pass ? "PASS" : "ABNORMAL");
        result.setDetails(details);
        result.setExecutedRules(executedRules);
        result.setExecutedAt(Instant.now());
        verifyMapper.insertCheckResult(result);
        return result;
    }

    /**
     * AMOUNT 核验：合同/发票/验收金额一致性
     */
    private VerifyCheckResult verifyAmount(Long applicationId) {
        List<ApplicationMaterial> materials = verifyMapper.selectMaterialsByApplication(applicationId);
        Map<String, BigDecimal> amounts = new HashMap<>();
        List<String> executedRules = new ArrayList<>();

        for (ApplicationMaterial material : materials) {
            MaterialRecognitionResult result = verifyMapper.selectRecognitionResult(material.getId());
            if (result != null && result.getAmount() != null) {
                amounts.put(material.getMaterialType(), result.getAmount());
            }
        }

        boolean pass = true;
        List<String> hints = new ArrayList<>();
        BigDecimal contract = amounts.get("CONTRACT");
        BigDecimal invoice = amounts.get("INVOICE");
        BigDecimal acceptance = amounts.get("ACCEPTANCE");

        if (contract != null && invoice != null) {
            BigDecimal diff = contract.subtract(invoice).abs();
            BigDecimal tolerance = contract.multiply(new BigDecimal("0.01"));
            if (diff.compareTo(tolerance) > 0) {
                pass = false;
                hints.add("合同金额与发票金额差异超过 1%");
                executedRules.add("R_AMOUNT_DIFF");
            }
        }
        if (invoice != null && acceptance != null && invoice.compareTo(acceptance) != 0) {
            pass = false;
            hints.add("发票金额与验收金额不一致");
            executedRules.add("R_AMOUNT_MATCH");
        }

        Map<String, Object> details = new HashMap<>();
        details.put("amounts", amounts);
        details.put("hints", hints);
        if (executedRules.isEmpty()) executedRules.add("R_AMOUNT_CONSISTENCY");

        VerifyCheckResult result = new VerifyCheckResult();
        result.setApplicationId(applicationId);
        result.setCheckType(CheckType.AMOUNT.name());
        result.setResult(pass ? "PASS" : "ABNORMAL");
        result.setDetails(details);
        result.setExecutedRules(executedRules);
        result.setExecutedAt(Instant.now());
        verifyMapper.insertCheckResult(result);
        return result;
    }

    /**
     * TIME 核验：时间逻辑
     */
    private VerifyCheckResult verifyTime(Long applicationId) {
        List<ApplicationMaterial> materials = verifyMapper.selectMaterialsByApplication(applicationId);
        Map<String, java.time.LocalDate> dates = new HashMap<>();
        List<String> executedRules = new ArrayList<>();

        for (ApplicationMaterial material : materials) {
            MaterialRecognitionResult result = verifyMapper.selectRecognitionResult(material.getId());
            if (result != null) {
                String type = material.getMaterialType();
                switch (type) {
                    case "CONTRACT" -> { if (result.getContractDate() != null) dates.put("CONTRACT", result.getContractDate()); }
                    case "INVOICE" -> { if (result.getInvoiceDate() != null) dates.put("INVOICE", result.getInvoiceDate()); }
                    case "LOGISTICS" -> { if (result.getLogisticsDate() != null) dates.put("LOGISTICS", result.getLogisticsDate()); }
                    case "ACCEPTANCE" -> { if (result.getAcceptanceDate() != null) dates.put("ACCEPTANCE", result.getAcceptanceDate()); }
                    case "PAYMENT" -> { if (result.getPaymentDate() != null) dates.put("PAYMENT", result.getPaymentDate()); }
                }
            }
        }

        boolean pass = true;
        List<String> hints = new ArrayList<>();
        java.time.LocalDate contract = dates.get("CONTRACT");
        java.time.LocalDate invoice = dates.get("INVOICE");
        java.time.LocalDate logistics = dates.get("LOGISTICS");
        java.time.LocalDate acceptance = dates.get("ACCEPTANCE");
        java.time.LocalDate payment = dates.get("PAYMENT");

        if (contract != null && invoice != null && invoice.isBefore(contract)) {
            pass = false;
            hints.add("发票日期早于合同日期");
            executedRules.add("R_TIME_ORDER");
        }
        if (invoice != null && logistics != null && logistics.isBefore(invoice)) {
            pass = false;
            hints.add("物流日期早于发票日期");
        }
        if (logistics != null && acceptance != null && acceptance.isBefore(logistics)) {
            pass = false;
            hints.add("验收日期早于物流日期");
        }
        if (acceptance != null && payment != null && payment.isBefore(acceptance)) {
            pass = false;
            hints.add("付款日期早于验收日期");
        }

        Map<String, Object> details = new HashMap<>();
        details.put("dates", dates);
        details.put("hints", hints);
        if (executedRules.isEmpty()) executedRules.add("R_TIME_LOGIC");

        VerifyCheckResult result = new VerifyCheckResult();
        result.setApplicationId(applicationId);
        result.setCheckType(CheckType.TIME.name());
        result.setResult(pass ? "PASS" : "ABNORMAL");
        result.setDetails(details);
        result.setExecutedRules(executedRules);
        result.setExecutedAt(Instant.now());
        verifyMapper.insertCheckResult(result);
        return result;
    }

    /**
     * REPEAT 核验：重复融资
     */
    private VerifyCheckResult verifyRepeat(Long applicationId) {
        FinancingApplication app = verifyMapper.selectApplicationById(applicationId);
        long count = verifyMapper.countApprovedApplicationsByEnterprise(app.getEnterpriseId(), app.getBusinessType());

        boolean pass = count == 0;
        Map<String, Object> details = new HashMap<>();
        details.put("existingApprovedCount", count);
        List<String> executedRules = new ArrayList<>();
        executedRules.add("R_REPEAT_FINANCING");

        VerifyCheckResult result = new VerifyCheckResult();
        result.setApplicationId(applicationId);
        result.setCheckType(CheckType.REPEAT.name());
        result.setResult(pass ? "PASS" : "ABNORMAL");
        result.setDetails(details);
        result.setExecutedRules(executedRules);
        result.setExecutedAt(Instant.now());
        verifyMapper.insertCheckResult(result);
        return result;
    }

    /**
     * 生成核验报告（快照不可篡改 + SHA-256 哈希）
     */
    @Transactional
    public VerifyReport generateReport(Long applicationId) {
        List<VerifyCheckResult> results = verifyMapper.selectCheckResultsByApplication(applicationId);

        // 综合评定
        long abnormalCount = results.stream().filter(r -> "ABNORMAL".equals(r.getResult())).count();
        String overall = abnormalCount == 0 ? "LOW" : abnormalCount <= 2 ? "MID" : "HIGH";

        // 风险提示
        List<String> riskHints = new ArrayList<>();
        for (VerifyCheckResult r : results) {
            if ("ABNORMAL".equals(r.getResult()) && r.getDetails() != null) {
                Object hints = r.getDetails().get("hints");
                if (hints instanceof List) {
                    riskHints.addAll((List<String>) hints);
                }
            }
        }

        // 快照（不可篡改）
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("applicationId", applicationId);
        snapshot.put("results", results);
        snapshot.put("overallAssessment", overall);
        snapshot.put("abnormalCount", abnormalCount);

        String contentHash;
        try {
            String json = objectMapper.writeValueAsString(snapshot);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(json.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            contentHash = sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("生成内容哈希失败", e);
        }

        VerifyReport report = new VerifyReport();
        report.setReportNo("RPT-" + System.currentTimeMillis());
        report.setApplicationId(applicationId);
        report.setVersion(1);
        report.setOverallAssessment(overall);
        report.setAbnormalCount((int) abnormalCount);
        report.setRiskHints(riskHints);
        report.setContentSnapshot(snapshot);
        report.setContentHash(contentHash);
        report.setGeneratedAt(Instant.now());

        verifyMapper.insertReport(report);
        log.info("[Verify] 核验报告已生成: appNo={}, overall={}, abnormal={}",
                applicationId, overall, abnormalCount);
        return report;
    }

    public VerifyReport getReportByApplication(Long applicationId) {
        return verifyMapper.selectReportByApplication(applicationId);
    }

    public List<VerifyCheckResult> getCheckResults(Long applicationId) {
        return verifyMapper.selectCheckResultsByApplication(applicationId);
    }
}
