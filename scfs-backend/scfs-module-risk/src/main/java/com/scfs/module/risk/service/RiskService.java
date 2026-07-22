package com.scfs.module.risk.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scfs.common.entity.RiskWeightConfig;
import com.scfs.common.enums.RiskLevel;
import com.scfs.common.service.RuleService;
import com.scfs.module.graph.entity.EnterprisePositionAnalysis;
import com.scfs.module.graph.service.GraphService;
import com.scfs.module.preaudit.entity.EnterpriseInfoConsistencyResult;
import com.scfs.module.preaudit.entity.MaterialCompletenessResult;
import com.scfs.module.preaudit.entity.MaterialValidityResult;
import com.scfs.module.preaudit.service.PreAuditService;
import com.scfs.module.risk.entity.RiskProfile;
import com.scfs.module.risk.entity.TransactionStability;
import com.scfs.module.risk.mapper.RiskMapper;
import com.scfs.module.verify.entity.FinancingApplication;
import com.scfs.module.verify.entity.VerifyCheckResult;
import com.scfs.module.verify.entity.VerifyReport;
import com.scfs.module.verify.mapper.VerifyMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

/**
 * 风险评分服务 - 对应 RFC 4.2.4 RiskService
 *
 * <p>三维加权评分：</p>
 * <ul>
 *   <li>供应链图谱分（40%）：基于位置分析 + 异常关系</li>
 *   <li>交易稳定性分（30%）：基于近 12 月交易笔数 + 金额标准差</li>
 *   <li>材料质量分（30%）：完整性 + 有效性 + 一致性</li>
 * </ul>
 *
 * <p>对应 RFC S2-11：RiskService.calculate(applicationId) → risk_profile</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RiskService {

    private final RiskMapper riskMapper;
    private final RuleService ruleService;
    private final VerifyMapper verifyMapper;
    private final PreAuditService preAuditService;
    private final GraphService graphService;
    private final ObjectMapper objectMapper;

    @Transactional
    public RiskProfile calculate(Long applicationId) {
        log.info("[Risk] 开始评分: applicationId={}", applicationId);

        FinancingApplication app = verifyMapper.selectApplicationById(applicationId);
        if (app == null) {
            throw new IllegalArgumentException("申请不存在");
        }

        RiskWeightConfig weightConfig = ruleService.getEnabledWeightConfig();
        if (weightConfig == null) {
            throw new IllegalStateException("未启用任何风险权重配置");
        }

        // 1. 供应链图谱分
        BigDecimal supplyChainScore = calculateSupplyChainScore(app.getEnterpriseId(), app.getEnterpriseId());
        // 2. 交易稳定性分
        BigDecimal transactionScore = calculateTransactionScore(app.getEnterpriseId());
        // 3. 材料质量分
        BigDecimal materialScore = calculateMaterialScore(applicationId);

        // 加权总分
        BigDecimal overall = supplyChainScore.multiply(BigDecimal.valueOf(weightConfig.getSupplyChainWeight()))
                .add(transactionScore.multiply(BigDecimal.valueOf(weightConfig.getTransactionWeight())))
                .add(materialScore.multiply(BigDecimal.valueOf(weightConfig.getMaterialWeight())))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        // 风险等级
        String riskLevel = determineRiskLevel(overall, weightConfig);

        // 风险原因 + 建议
        List<String> riskReasons = collectRiskReasons(applicationId, supplyChainScore, transactionScore, materialScore);
        List<String> suggestions = generateSuggestions(riskLevel, riskReasons);

        // 内容哈希
        String contentHash = generateContentHash(applicationId, supplyChainScore, transactionScore, materialScore, overall);

        RiskProfile profile = new RiskProfile();
        profile.setApplicationId(applicationId);
        profile.setEnterpriseId(app.getEnterpriseId());
        profile.setVersion(1);
        profile.setSupplyChainScore(supplyChainScore);
        profile.setTransactionScore(transactionScore);
        profile.setMaterialScore(materialScore);
        profile.setWeightedConfigId(weightConfig.getId());
        profile.setOverallScore(overall);
        profile.setRiskLevel(riskLevel);
        profile.setRiskReasons(riskReasons);
        profile.setSuggestions(suggestions);
        profile.setContentHash(contentHash);
        profile.setGeneratedAt(Instant.now());

        riskMapper.insertRiskProfile(profile);
        log.info("[Risk] 评分完成: applicationId={}, overall={}, level={}",
                applicationId, overall, riskLevel);
        return profile;
    }

    public RiskProfile getProfileByApplication(Long applicationId) {
        return riskMapper.selectRiskProfileByApplication(applicationId);
    }

    public List<RiskProfile> getProfilesByEnterprise(Long enterpriseId) {
        return riskMapper.selectRiskProfilesByEnterprise(enterpriseId);
    }

    // ========== 供应链图谱分 ==========
    private BigDecimal calculateSupplyChainScore(Long enterpriseId, Long coreEnterpriseId) {
        EnterprisePositionAnalysis position = graphService.getPositionAnalysis(enterpriseId);
        if (position == null) {
            // 临时计算
            graphService.analyzeEnterprisePosition(enterpriseId, coreEnterpriseId);
            position = graphService.getPositionAnalysis(enterpriseId);
        }

        BigDecimal score = BigDecimal.valueOf(60);  // 基础分
        if (position != null) {
            if (Boolean.TRUE.equals(position.getInCoreChain())) {
                score = score.add(BigDecimal.valueOf(20));
            }
            if (Boolean.TRUE.equals(position.getUpstreamStable())) {
                score = score.add(BigDecimal.valueOf(10));
            }
            if (Boolean.TRUE.equals(position.getDownstreamStable())) {
                score = score.add(BigDecimal.valueOf(10));
            }
            if ("HIGH".equals(position.getCredibility())) {
                score = score.add(BigDecimal.valueOf(10));
            } else if ("MID".equals(position.getCredibility())) {
                score = score.add(BigDecimal.valueOf(5));
            }
        }
        return score.min(BigDecimal.valueOf(100));
    }

    // ========== 交易稳定性分 ==========
    private BigDecimal calculateTransactionScore(Long enterpriseId) {
        TransactionStability stability = riskMapper.selectTransactionStability(enterpriseId);
        if (stability != null) {
            return stability.getScore();
        }
        // Mock：60-80 分随机
        return BigDecimal.valueOf(60 + (long) (Math.random() * 20));
    }

    // ========== 材料质量分 ==========
    private BigDecimal calculateMaterialScore(Long applicationId) {
        BigDecimal score = BigDecimal.valueOf(100);

        MaterialCompletenessResult completeness = preAuditService.getCompleteness(applicationId);
        if (completeness != null && completeness.getCompletenessPct() != null) {
            score = score.multiply(completeness.getCompletenessPct())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }

        MaterialValidityResult validity = preAuditService.getValidity(applicationId);
        if (validity != null) {
            int abnormal = validity.getAbnormalCount() == null ? 0 : validity.getAbnormalCount();
            score = score.subtract(BigDecimal.valueOf(abnormal * 5));
        }

        EnterpriseInfoConsistencyResult consistency = preAuditService.getConsistency(applicationId);
        if (consistency != null) {
            int mismatch = consistency.getMismatchCount() == null ? 0 : consistency.getMismatchCount();
            score = score.subtract(BigDecimal.valueOf(mismatch * 10));
        }

        return score.max(BigDecimal.ZERO).min(BigDecimal.valueOf(100));
    }

    private String determineRiskLevel(BigDecimal overall, RiskWeightConfig config) {
        if (overall.compareTo(BigDecimal.valueOf(config.getLowRiskThreshold())) >= 0) {
            return RiskLevel.LOW.name();
        } else if (overall.compareTo(BigDecimal.valueOf(config.getMidRiskThreshold())) >= 0) {
            return RiskLevel.MID.name();
        } else if (overall.compareTo(BigDecimal.valueOf(config.getHighRiskThreshold())) >= 0) {
            return RiskLevel.HIGH.name();
        } else {
            return RiskLevel.EXTREME.name();
        }
    }

    private List<String> collectRiskReasons(Long applicationId, BigDecimal supplyChainScore,
                                              BigDecimal transactionScore, BigDecimal materialScore) {
        List<String> reasons = new ArrayList<>();
        if (supplyChainScore.compareTo(BigDecimal.valueOf(60)) < 0) {
            reasons.add("供应链图谱评分较低: " + supplyChainScore);
        }
        if (transactionScore.compareTo(BigDecimal.valueOf(60)) < 0) {
            reasons.add("交易稳定性评分较低: " + transactionScore);
        }
        if (materialScore.compareTo(BigDecimal.valueOf(70)) < 0) {
            reasons.add("材料质量评分较低: " + materialScore);
        }
        // 加入核验异常
        List<VerifyCheckResult> checkResults = verifyMapper.selectCheckResultsByApplication(applicationId);
        for (VerifyCheckResult r : checkResults) {
            if ("ABNORMAL".equals(r.getResult()) && r.getDetails() != null) {
                Object hints = r.getDetails().get("hints");
                if (hints instanceof List) {
                    reasons.addAll((List<String>) hints);
                }
            }
        }
        return reasons;
    }

    private List<String> generateSuggestions(String riskLevel, List<String> reasons) {
        List<String> suggestions = new ArrayList<>();
        switch (riskLevel) {
            case "LOW" -> suggestions.add("风险等级低，建议正常推进审批流程");
            case "MID" -> {
                suggestions.add("风险等级中等，建议关注以下事项");
                suggestions.addAll(reasons);
            }
            case "HIGH" -> {
                suggestions.add("风险等级高，建议加强风控审查");
                suggestions.addAll(reasons);
            }
            case "EXTREME" -> {
                suggestions.add("风险等级极高，建议拒绝或要求增信措施");
                suggestions.addAll(reasons);
            }
        }
        return suggestions;
    }

    private String generateContentHash(Long applicationId, BigDecimal supplyChainScore,
                                        BigDecimal transactionScore, BigDecimal materialScore,
                                        BigDecimal overall) {
        try {
            String json = String.format("{\"applicationId\":%d,\"supplyChain\":%s,\"transaction\":%s,\"material\":%s,\"overall\":%s}",
                    applicationId, supplyChainScore, transactionScore, materialScore, overall);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(json.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("生成内容哈希失败", e);
        }
    }
}
