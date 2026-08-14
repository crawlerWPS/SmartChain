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
import java.nio.charset.StandardCharsets;
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
        FinancingApplication application = verifyMapper.selectApplicationById(applicationId);
        if (application == null) throw new IllegalArgumentException("融资申请不存在");
        verifyMapper.deleteCheckResultsByApplication(applicationId);
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
        FinancingApplication application = verifyMapper.selectApplicationById(applicationId);
        List<ApplicationMaterial> materials = verifyMapper.selectMaterialsByApplication(applicationId);
        List<Map<String, Object>> comparisons = new ArrayList<>();
        boolean hasComparableMaterial = false;
        boolean mismatch = false;
        boolean missingField = false;
        List<String> executedRules = new ArrayList<>();

        for (ApplicationMaterial material : materials) {
            if (!Set.of("CONTRACT", "INVOICE").contains(material.getMaterialType())) continue;
            MaterialRecognitionResult result = verifyMapper.selectRecognitionResult(material.getId());
            if (result == null) continue;
            hasComparableMaterial = true;
            boolean buyerPresent = hasText(result.getBuyerName()) || hasText(result.getBuyerUscc());
            boolean sellerPresent = hasText(result.getSellerName()) || hasText(result.getSellerUscc());
            boolean buyerMatch = partyMatches(application.getBuyerName(), application.getBuyerUscc(), result.getBuyerName(), result.getBuyerUscc());
            boolean sellerMatch = partyMatches(application.getSellerName(), application.getSellerUscc(), result.getSellerName(), result.getSellerUscc());
            missingField |= !buyerPresent || !sellerPresent;
            mismatch |= (buyerPresent && !buyerMatch) || (sellerPresent && !sellerMatch);
            Map<String, Object> comparison = new LinkedHashMap<>();
            comparison.put("materialId", material.getId());
            comparison.put("materialType", material.getMaterialType());
            comparison.put("ocrBuyerName", result.getBuyerName());
            comparison.put("ocrBuyerUscc", result.getBuyerUscc());
            comparison.put("buyerMatch", buyerMatch);
            comparison.put("ocrSellerName", result.getSellerName());
            comparison.put("ocrSellerUscc", result.getSellerUscc());
            comparison.put("sellerMatch", sellerMatch);
            comparisons.add(comparison);
        }

        boolean missing = !hasComparableMaterial || missingField;
        boolean pass = !missing && !mismatch;
        Map<String, Object> details = new HashMap<>();
        details.put("applicationBuyer", partyDetail(application.getBuyerName(), application.getBuyerUscc()));
        details.put("applicationSeller", partyDetail(application.getSellerName(), application.getSellerUscc()));
        details.put("comparisons", comparisons);
        List<String> hints = new ArrayList<>();
        if (!hasComparableMaterial) hints.add("没有已完成OCR识别的合同或发票");
        if (missingField) hints.add("合同或发票未完整识别买卖方信息");
        if (mismatch) hints.add("合同或发票中的买卖方与融资申请登记客户不一致");
        details.put("hints", hints);
        executedRules.add("R_SUBJECT_CONSISTENCY");

        VerifyCheckResult result = new VerifyCheckResult();
        result.setApplicationId(applicationId);
        result.setCheckType(CheckType.SUBJECT.name());
        result.setResult(mismatch ? "ABNORMAL" : missing ? "MISSING" : pass ? "PASS" : "ABNORMAL");
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
        FinancingApplication application = verifyMapper.selectApplicationById(applicationId);
        List<ApplicationMaterial> materials = verifyMapper.selectMaterialsByApplication(applicationId);
        Map<String, BigDecimal> amounts = new HashMap<>();
        List<Map<String, Object>> materialAmounts = new ArrayList<>();
        List<String> executedRules = new ArrayList<>();

        for (ApplicationMaterial material : materials) {
            MaterialRecognitionResult result = verifyMapper.selectRecognitionResult(material.getId());
            if (result != null && result.getAmount() != null) {
                amounts.put(material.getMaterialType(), result.getAmount());
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("materialId", material.getId());
                row.put("fileName", material.getFileName());
                row.put("materialType", material.getMaterialType());
                row.put("ocrAmount", result.getAmount());
                row.put("financingAmountMatch", application.getFinancingAmount() == null
                        || application.getFinancingAmount().compareTo(result.getAmount()) <= 0);
                materialAmounts.add(row);
            }
        }

        BigDecimal contract = amounts.get("CONTRACT");
        BigDecimal invoice = amounts.get("INVOICE");
        BigDecimal acceptance = amounts.get("ACCEPTANCE");
        boolean missing = contract == null || invoice == null;
        boolean pass = !missing;
        boolean amountViolation = false;
        List<String> hints = new ArrayList<>();
        if (contract == null) hints.add("缺少合同金额或合同尚未完成识别");
        if (invoice == null) hints.add("缺少发票金额或发票尚未完成识别");

        BigDecimal financingAmount = application.getFinancingAmount();
        if (financingAmount != null && contract != null && financingAmount.compareTo(contract) > 0) {
            pass = false;
            amountViolation = true;
            hints.add("融资申请金额大于合同金额");
            executedRules.add("R_FINANCING_AMOUNT_CONTRACT");
        }
        if (financingAmount != null && invoice != null && financingAmount.compareTo(invoice) > 0) {
            pass = false;
            amountViolation = true;
            hints.add("融资申请金额大于发票金额");
            executedRules.add("R_FINANCING_AMOUNT_INVOICE");
        }

        if (contract != null && invoice != null) {
            if (contract.compareTo(invoice) != 0) {
                pass = false;
                amountViolation = true;
                hints.add("合同金额与发票金额不一致");
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
        details.put("financingAmount", financingAmount);
        details.put("contractInvoiceMatch", contract != null && invoice != null && contract.compareTo(invoice) == 0);
        details.put("materialAmounts", materialAmounts);
        details.put("hints", hints);
        if (executedRules.isEmpty()) executedRules.add("R_AMOUNT_CONSISTENCY");

        VerifyCheckResult result = new VerifyCheckResult();
        result.setApplicationId(applicationId);
        result.setCheckType(CheckType.AMOUNT.name());
        result.setResult(amountViolation ? "ABNORMAL" : missing ? "MISSING" : pass ? "PASS" : "ABNORMAL");
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
        FinancingApplication application = verifyMapper.selectApplicationById(applicationId);
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

        java.time.LocalDate contract = dates.get("CONTRACT");
        java.time.LocalDate invoice = dates.get("INVOICE");
        java.time.LocalDate logistics = dates.get("LOGISTICS");
        java.time.LocalDate acceptance = dates.get("ACCEPTANCE");
        java.time.LocalDate payment = dates.get("PAYMENT");
        boolean missing = contract == null || invoice == null;
        boolean pass = !missing;
        List<String> hints = new ArrayList<>();
        if (contract == null) hints.add("缺少合同日期或合同尚未完成识别");
        if (invoice == null) hints.add("缺少发票日期或发票尚未完成识别");

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
        Map<String, String> serializableDates = new HashMap<>();
        dates.forEach((key, value) -> serializableDates.put(key, value.toString()));
        details.put("dates", serializableDates);
        details.put("applicationSubmittedAt", application == null ? null : application.getSubmittedAt());
        List<Map<String, Object>> materialDates = new ArrayList<>();
        for (ApplicationMaterial material : materials) {
            java.time.LocalDate date = dates.get(material.getMaterialType());
            if (date != null) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("materialId", material.getId());
                row.put("fileName", material.getFileName());
                row.put("materialType", material.getMaterialType());
                row.put("ocrDate", date.toString());
                materialDates.add(row);
            }
        }
        List<Map<String, Object>> comparisons = new ArrayList<>();
        addTimeComparison(comparisons, "合同日期应不晚于发票日期", contract, invoice);
        addTimeComparison(comparisons, "发票日期应不晚于物流日期", invoice, logistics);
        addTimeComparison(comparisons, "物流日期应不晚于验收日期", logistics, acceptance);
        addTimeComparison(comparisons, "验收日期应不晚于付款日期", acceptance, payment);
        details.put("materialDates", materialDates);
        details.put("comparisons", comparisons);
        details.put("hints", hints);
        if (executedRules.isEmpty()) executedRules.add("R_TIME_LOGIC");

        VerifyCheckResult result = new VerifyCheckResult();
        result.setApplicationId(applicationId);
        result.setCheckType(CheckType.TIME.name());
        result.setResult(missing ? "MISSING" : pass ? "PASS" : "ABNORMAL");
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
        List<FinancingApplication> matched = verifyMapper.selectApprovedApplicationsByEnterprise(
                app.getEnterpriseId(), app.getBusinessType(), applicationId);
        long count = matched.size();

        boolean pass = count == 0;
        Map<String, Object> details = new HashMap<>();
        details.put("existingApprovedCount", count);
        Map<String, Object> current = new LinkedHashMap<>();
        current.put("id", app.getId());
        current.put("appNo", app.getAppNo());
        current.put("businessType", app.getBusinessType());
        current.put("financingAmount", app.getFinancingAmount());
        current.put("buyerName", app.getBuyerName());
        current.put("sellerName", app.getSellerName());
        details.put("currentApplication", current);
        List<Map<String, Object>> matchedApplications = new ArrayList<>();
        for (FinancingApplication historical : matched) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", historical.getId());
            item.put("appNo", historical.getAppNo());
            item.put("businessType", historical.getBusinessType());
            item.put("financingAmount", historical.getFinancingAmount());
            item.put("status", historical.getStatus());
            item.put("approvedAt", historical.getApprovedAt());
            matchedApplications.add(item);
        }
        details.put("matchedApplications", matchedApplications);
        details.put("hints", count == 0 ? List.of() : List.of("发现同一融资企业、同一业务类型的已审批申请"));
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

    private void addTimeComparison(List<Map<String, Object>> comparisons, String message,
                                   java.time.LocalDate left, java.time.LocalDate right) {
        if (left == null || right == null) return;
        Map<String, Object> comparison = new LinkedHashMap<>();
        comparison.put("message", message);
        comparison.put("leftDate", left.toString());
        comparison.put("rightDate", right.toString());
        comparison.put("match", !right.isBefore(left));
        comparisons.add(comparison);
    }

    /**
     * 生成核验报告（快照不可篡改 + SHA-256 哈希）
     */
    @Transactional
    public VerifyReport generateReport(Long applicationId) {
        List<VerifyCheckResult> results = verifyMapper.selectCheckResultsByApplication(applicationId);

        // 综合评定
        long abnormalCount = results.stream().filter(r -> !"PASS".equals(r.getResult())).count();
        String overall = abnormalCount == 0 ? "LOW" : abnormalCount <= 2 ? "MID" : "HIGH";

        // 风险提示
        List<String> riskHints = new ArrayList<>();
        for (VerifyCheckResult r : results) {
            if (!"PASS".equals(r.getResult()) && r.getDetails() != null) {
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

    /** 导出一个轻量、可下载的 PDF 报告，避免依赖浏览器页面截图或额外 PDF 引擎。 */
    public byte[] exportReportPdf(String reportNo) {
        VerifyReport report = verifyMapper.selectReportByNo(reportNo);
        if (report == null) {
            throw new IllegalArgumentException("报告不存在: " + reportNo);
        }
        return buildStyledPdf(report);
    }

    @SuppressWarnings("unchecked")
    private void appendCheckDetails(List<String> lines, Map<String, Object> snapshot) {
        Object rawResults = snapshot == null ? null : snapshot.get("results");
        if (!(rawResults instanceof List<?> results) || results.isEmpty()) {
            lines.add("暂无核验明细");
            return;
        }
        for (Object raw : results) {
            if (!(raw instanceof Map<?, ?> result)) continue;
            String checkType = String.valueOf(result.containsKey("checkType") ? result.get("checkType") : "-");
            String status = String.valueOf(result.containsKey("result") ? result.get("result") : "-");
            lines.add("核验项目：" + checkTypeLabel(checkType));
            lines.add("核验结果：" + resultLabel(status));
            lines.add("核验结论：" + conclusionLabel(checkType, status));
        }
    }

    private String conclusionLabel(String type, String result) {
        if (!"PASS".equals(result)) {
            return switch (type) {
                case "SUBJECT" -> "发现主体信息异常，请人工复核买卖双方资料。";
                case "AMOUNT" -> "发现交易金额异常，请核对合同、发票及订单金额。";
                case "TIME" -> "发现交易时间逻辑异常，请核对业务材料时间顺序。";
                case "REPEAT" -> "发现重复融资风险，请核对历史融资记录。";
                default -> "该核验项目未通过，请人工复核。";
            };
        }
        return switch (type) {
            case "SUBJECT" -> "买卖双方主体信息一致，未发现异常。";
            case "AMOUNT" -> "合同、订单及发票金额未发现明显差异。";
            case "TIME" -> "交易材料时间顺序合理，未发现明显异常。";
            case "REPEAT" -> "未发现同企业已审批的重复融资记录。";
            default -> "该核验项目已通过。";
        };
    }

    private String checkTypeLabel(String type) {
        return switch (type) {
            case "SUBJECT" -> "主体一致性";
            case "AMOUNT" -> "金额一致性";
            case "TIME" -> "时间逻辑";
            case "REPEAT" -> "重复融资";
            default -> type;
        };
    }

    private String resultLabel(String result) {
        return switch (result) {
            case "PASS" -> "通过";
            case "ABNORMAL" -> "异常";
            case "MISSING" -> "缺失";
            default -> result;
        };
    }

    private String assessmentLabel(String assessment) {
        if ("LOW".equals(assessment)) return "低风险";
        if ("MID".equals(assessment)) return "中风险";
        if ("HIGH".equals(assessment)) return "高风险";
        return assessment == null ? "-" : assessment;
    }

    private byte[] buildStyledPdf(VerifyReport report) {
        PdfDocument pdf = new PdfDocument();
        PdfPage page = pdf.newPage();
        drawReportHeader(page, report);

        List<Map<String, Object>> results = reportResults(report.getContentSnapshot());
        long passed = results.stream().filter(r -> "PASS".equals(String.valueOf(r.get("result")))).count();
        drawSummaryCards(page, report, passed, results.size());
        drawMetadata(page, report);

        if (report.getRiskHints() != null && !report.getRiskHints().isEmpty()) {
            page = page.ensureSpace(56 + wrappedLineCount(report.getRiskHints(), 52) * 14, pdf);
            page.sectionTitle("风险提示");
            page.alertBox(report.getRiskHints(), report.getAbnormalCount() > 0);
        }

        page = page.ensureSpace(50, pdf);
        page.sectionTitle("核验结果");
        if (results.isEmpty()) {
            page.mutedText("暂无核验明细", 52, page.y - 12, 11);
            page.y -= 32;
        } else {
            for (Map<String, Object> item : results) {
                page = page.ensureSpace(checkCardHeight(item), pdf);
                drawCheckCard(page, item);
            }
        }
        return pdf.build();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> reportResults(Map<String, Object> snapshot) {
        if (snapshot == null || !(snapshot.get("results") instanceof List<?> raw)) return List.of();
        return raw.stream().filter(Map.class::isInstance).map(v -> (Map<String, Object>) v).toList();
    }

    private void drawReportHeader(PdfPage page, VerifyReport report) {
        page.fillRect(0, 750, 595, 92, "0.09 0.32 0.76");
        page.text("SCFS 供应链金融风控平台", 42, 812, 10, "0.80 0.88 1");
        page.text("真实性核验报告", 42, 780, 22, "1 1 1");
        page.text("报告编号：" + report.getReportNo() + "    版本：v" + report.getVersion(), 42, 758, 9, "0.88 0.93 1");
        page.y = 724;
    }

    private void drawSummaryCards(PdfPage page, VerifyReport report, long passed, int total) {
        page.summaryCard(42, page.y - 70, 158, 66, "总体评估", assessmentLabel(report.getOverallAssessment()),
                "HIGH".equals(report.getOverallAssessment()) ? "0.81 0.07 0.13" : "MID".equals(report.getOverallAssessment()) ? "0.83 0.42 0.03" : "0.22 0.56 0.05");
        page.summaryCard(218, page.y - 70, 158, 66, "核验通过", passed + " / " + total, "0.22 0.56 0.05");
        page.summaryCard(394, page.y - 70, 158, 66, "异常项目", String.valueOf(report.getAbnormalCount()),
                report.getAbnormalCount() > 0 ? "0.81 0.07 0.13" : "0.22 0.56 0.05");
        page.y -= 88;
    }

    private void drawMetadata(PdfPage page, VerifyReport report) {
        page.fillRect(42, page.y - 62, 510, 62, "0.97 0.98 1");
        page.strokeRect(42, page.y - 62, 510, 62, "0.84 0.89 0.98");
        page.mutedText("申请编号", 58, page.y - 22, 9);
        page.text("#" + report.getApplicationId(), 58, page.y - 43, 11, "0.12 0.16 0.23");
        page.mutedText("报告版本", 225, page.y - 22, 9);
        page.text("v" + report.getVersion(), 225, page.y - 43, 11, "0.12 0.16 0.23");
        page.mutedText("生成时间", 382, page.y - 22, 9);
        page.text(String.valueOf(report.getGeneratedAt()).replace('T', ' '), 382, page.y - 43, 9, "0.12 0.16 0.23");
        page.y -= 82;
    }

    private int checkCardHeight(Map<String, Object> item) {
        String type = String.valueOf(item.getOrDefault("checkType", "-"));
        String status = String.valueOf(item.getOrDefault("result", "-"));
        List<String> hints = detailHints(item);
        String conclusion = "PASS".equals(status) || hints.isEmpty()
                ? conclusionLabel(type, status) : hints.get(0);
        int conclusionLines = wrap(conclusion, 45).size();
        int hintLines = wrappedLineCount(hints, 50);
        return 72 + conclusionLines * 15 + (hintLines == 0 ? 0 : 8 + hintLines * 14);
    }

    private void drawCheckCard(PdfPage page, Map<String, Object> item) {
        String type = String.valueOf(item.getOrDefault("checkType", "-"));
        String status = String.valueOf(item.getOrDefault("result", "-"));
        boolean passed = "PASS".equals(status);
        List<String> hints = detailHints(item);
        int height = checkCardHeight(item);
        double bottom = page.y - height;
        page.fillRect(42, bottom, 510, height - 8, "1 1 1");
        page.fillRect(42, bottom, 4, height - 8, passed ? "0.32 0.77 0.10" : "1 0.30 0.31");
        page.strokeRect(42, bottom, 510, height - 8, "0.88 0.90 0.93");
        page.text(checkTypeLabel(type), 58, page.y - 28, 13, "0.12 0.16 0.23");
        page.badge(resultLabel(status), 455, page.y - 35, passed);
        String conclusion = passed ? conclusionLabel(type, status)
                : hints.isEmpty() ? conclusionLabel(type, status) : hints.get(0);
        List<String> conclusionLines = wrap(conclusion, 45);
        double textY = page.y - 52;
        for (String line : conclusionLines) {
            page.text(line, 58, textY, 10, "0.24 0.29 0.37");
            textY -= 15;
        }
        if (!hints.isEmpty()) {
            double hintY = textY - 3;
            for (String hint : hints) {
                for (String line : wrap("- " + hint, 50)) {
                    page.text(line, 64, hintY, 9, "0.45 0.49 0.56");
                    hintY -= 14;
                }
            }
        }
        page.y -= height;
    }

    @SuppressWarnings("unchecked")
    private List<String> detailHints(Map<String, Object> item) {
        if (!(item.get("details") instanceof Map<?, ?> details) || !(details.get("hints") instanceof List<?> hints)) return List.of();
        return hints.stream().map(String::valueOf).toList();
    }

    private int wrappedLineCount(List<String> values, int maxChars) {
        return values.stream().mapToInt(value -> wrap(value, maxChars).size()).sum();
    }

    private class PdfDocument {
        private final List<PdfPage> pages = new ArrayList<>();
        PdfPage newPage() { PdfPage page = new PdfPage(this); pages.add(page); return page; }
        byte[] build() {
            for (int i = 0; i < pages.size(); i++) pages.get(i).footer(i + 1, pages.size());
            List<String> objects = new ArrayList<>();
            objects.add("<< /Type /Catalog /Pages 2 0 R >>");
            StringBuilder kids = new StringBuilder();
            for (int i = 0; i < pages.size(); i++) kids.append(5 + i * 2).append(" 0 R ");
            objects.add("<< /Type /Pages /Kids [" + kids + "] /Count " + pages.size() + " >>");
            objects.add("<< /Type /Font /Subtype /Type0 /BaseFont /STSong-Light /Encoding /UniGB-UCS2-H /DescendantFonts [4 0 R] >>");
            objects.add("<< /Type /Font /Subtype /CIDFontType0 /BaseFont /STSong-Light /CIDSystemInfo << /Registry (Adobe) /Ordering (GB1) /Supplement 4 >> /DW 1000 >>");
            for (int i = 0; i < pages.size(); i++) {
                int streamId = 6 + i * 2;
                String stream = pages.get(i).content.toString();
                objects.add("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 3 0 R >> >> /Contents " + streamId + " 0 R >>");
                objects.add("<< /Length " + stream.getBytes(StandardCharsets.US_ASCII).length + " >>\nstream\n" + stream + "endstream");
            }
            return serializePdf(objects);
        }
    }

    private class PdfPage {
        private final StringBuilder content = new StringBuilder();
        private final PdfDocument document;
        private double y = 790;
        PdfPage(PdfDocument document) { this.document = document; }
        PdfPage ensureSpace(double needed, PdfDocument pdf) {
            if (y - needed < 58) {
                PdfPage next = pdf.newPage();
                next.fillRect(0, 806, 595, 36, "0.09 0.32 0.76");
                next.text("SCFS 真实性核验报告（续）", 42, 818, 11, "1 1 1");
                next.y = 780;
                return next;
            }
            return this;
        }
        void text(String value, double x, double y, int size, String color) {
            content.append("BT /F1 ").append(size).append(" Tf ").append(color).append(" rg 1 0 0 1 ")
                    .append(x).append(' ').append(y).append(" Tm <FEFF").append(toUtf16Hex(value)).append("> Tj ET\n");
        }
        void mutedText(String value, double x, double y, int size) { text(value, x, y, size, "0.45 0.49 0.56"); }
        void fillRect(double x, double y, double w, double h, String color) { content.append("q ").append(color).append(" rg ").append(x).append(' ').append(y).append(' ').append(w).append(' ').append(h).append(" re f Q\n"); }
        void strokeRect(double x, double y, double w, double h, String color) { content.append("q ").append(color).append(" RG 0.8 w ").append(x).append(' ').append(y).append(' ').append(w).append(' ').append(h).append(" re S Q\n"); }
        void summaryCard(double x, double y, double w, double h, String title, String value, String valueColor) { fillRect(x,y,w,h,"1 1 1"); strokeRect(x,y,w,h,"0.84 0.89 0.98"); mutedText(title,x+14,y+43,9); text(value,x+14,y+17,16,valueColor); }
        void sectionTitle(String title) { text(title, 42, y - 16, 14, "0.12 0.16 0.23"); fillRect(42, y - 23, 32, 2, "0.09 0.32 0.76"); y -= 38; }
        void alertBox(List<String> items, boolean warning) { int h=24+wrappedLineCount(items,52)*14; fillRect(42,y-h,510,h,warning?"1 0.97 0.90":"0.95 1 0.94"); strokeRect(42,y-h,510,h,warning?"1 0.79 0.34":"0.62 0.85 0.47"); double ty=y-22; for(String item:items){for(String line:wrap("- "+item,52)){text(line,56,ty,9,"0.31 0.31 0.31");ty-=14;}} y-=h+18; }
        void badge(String value, double x, double y, boolean passed) { fillRect(x,y,72,22,passed?"0.90 0.98 0.86":"1 0.91 0.91"); text(value,x+22,y+6,9,passed?"0.22 0.56 0.05":"0.81 0.07 0.13"); }
        void wrappedText(String value,double x,double y,double width,int size,String color,int leading){int max=Math.max(8,(int)(width/size));List<String> lines=wrap(value,max);double ty=y;for(String line:lines){text(line,x,ty,size,color);ty-=leading;}}
        void footer(int current, int total) { mutedText("SCFS 供应链金融风控平台",42,28,8); mutedText("第 " + current + " / " + total + " 页",500,28,8); fillRect(42,42,510,0.5,"0.88 0.90 0.93"); }
    }

    private List<String> wrap(String value, int maxChars) {
        if (value == null || value.isBlank()) return List.of("-");
        List<String> lines = new ArrayList<>();
        for (int start = 0; start < value.length(); start += maxChars) lines.add(value.substring(start, Math.min(value.length(), start + maxChars)));
        return lines;
    }

    private byte[] serializePdf(List<String> objects) {
        StringBuilder pdf = new StringBuilder("%PDF-1.4\n");
        List<Integer> offsets = new ArrayList<>();
        for (int i = 0; i < objects.size(); i++) {
            offsets.add(pdf.toString().getBytes(StandardCharsets.US_ASCII).length);
            pdf.append(i + 1).append(" 0 obj\n").append(objects.get(i)).append("\nendobj\n");
        }
        int xref = pdf.toString().getBytes(StandardCharsets.US_ASCII).length;
        pdf.append("xref\n0 ").append(objects.size() + 1).append("\n0000000000 65535 f \n");
        for (Integer offset : offsets) {
            pdf.append(String.format("%010d 00000 n \n", offset));
        }
        pdf.append("trailer\n<< /Size ").append(objects.size() + 1).append(" /Root 1 0 R >>\nstartxref\n")
                .append(xref).append("\n%%EOF\n");
        return pdf.toString().getBytes(StandardCharsets.US_ASCII);
    }

    private String toUtf16Hex(String value) {
        byte[] bytes = (value == null ? "" : value).getBytes(StandardCharsets.UTF_16BE);
        StringBuilder hex = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            hex.append(String.format("%02X", b & 0xFF));
        }
        return hex.toString();
    }

    public List<VerifyCheckResult> getCheckResults(Long applicationId) {
        return verifyMapper.selectCheckResultsByApplication(applicationId);
    }

    private String normalizePartyName(String value) {
        return value.replaceFirst("^(名称|甲方|乙方|买方|卖方|购买方|销售方)[：:]*\\s*", "")
                .replaceAll("\\s+", "").trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private boolean partyMatches(String expectedName, String expectedUscc, String actualName, String actualUscc) {
        if (hasText(expectedUscc) && hasText(actualUscc)) {
            return expectedUscc.replaceAll("\\s+", "").equalsIgnoreCase(actualUscc.replaceAll("\\s+", ""));
        }
        if (!hasText(expectedName) || !hasText(actualName)) return false;
        String expected = normalizePartyName(expectedName);
        String actual = normalizePartyName(actualName);
        return actual.equals(expected) || actual.contains(expected) || expected.contains(actual);
    }

    private Map<String, Object> partyDetail(String name, String uscc) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("name", name);
        detail.put("uscc", uscc);
        return detail;
    }
}
