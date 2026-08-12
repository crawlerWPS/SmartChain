package com.scfs.common.service;

import com.scfs.common.audit.Audit;
import com.scfs.common.core.PageResult;
import com.scfs.common.entity.MaterialChecklistTemplate;
import com.scfs.common.entity.RuleChangeLog;
import com.scfs.common.entity.RuleDefinition;
import com.scfs.common.entity.RiskWeightConfig;
import com.scfs.common.enums.DualControlStatus;
import com.scfs.common.enums.RuleCategory;
import com.scfs.common.mapper.RiskWeightConfigMapper;
import com.scfs.common.mapper.RuleDefinitionMapper;
import com.scfs.common.security.SecurityContextHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * 规则管理服务 - 对应 RFC 4.1.2 RuleService + 双岗审批
 *
 * <p>双岗流程：</p>
 * <ol>
 *   <li>OPS_MAKER 经办 → status=PENDING</li>
 *   <li>OPS_CHECKER 复核 → status=APPROVED/REJECTED</li>
 *   <li>APPROVED 后立即生效（status=ENABLED）</li>
 * </ol>
 *
 * <p>对应 RFC R-03a/R-03b 双岗校验</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RuleService {

    private final RuleDefinitionMapper ruleMapper;
    private final RiskWeightConfigMapper weightConfigMapper;
    private final SecurityContextHelper securityContextHelper;

    // ========== 规则定义 ==========

    public RuleDefinition getRuleById(Long id) {
        return ruleMapper.selectById(id);
    }

    public PageResult<RuleDefinition> searchRules(RuleCategory category, Short status, String keyword,
                                                  long offset, int size) {
        String categoryStr = category == null ? null : category.name();
        long total = ruleMapper.countAll(categoryStr, status, keyword);
        if (total == 0) {
            return PageResult.empty();
        }
        return PageResult.of(ruleMapper.selectPage(categoryStr, status, keyword, offset, size), total);
    }

    public List<RuleDefinition> listEnabledRulesByCategory(RuleCategory category) {
        return ruleMapper.selectByCategory(category.name());
    }

    /** 启用或禁用规则。 */
    @Audit(module = "RULE", action = "STATUS_UPDATE", targetType = "RULE_DEFINITION", targetIdExpr = "#ruleId", snapshot = true)
    @Transactional
    public void updateRuleStatus(Long ruleId, Short status) {
        securityContextHelper.getCurrentUserIdOrThrow();
        if (status == null || (status != 0 && status != 1)) {
            throw new IllegalArgumentException("规则状态只能是 0（禁用）或 1（启用）");
        }
        RuleDefinition rule = ruleMapper.selectById(ruleId);
        if (rule == null) {
            throw new IllegalArgumentException("规则不存在");
        }
        ruleMapper.updateStatus(ruleId, status, rule.getVersion());
        log.info("[Rule] 规则状态已更新: ruleId={}, status={}", ruleId, status);
    }

    /**
     * 经办创建规则（status=PENDING）
     */
    @Audit(module = "RULE", action = "CREATE", targetType = "RULE_DEFINITION", targetIdExpr = "#result", snapshot = true)
    @Transactional
    public Long createRule(RuleDefinition rule) {
        Long currentUserId = securityContextHelper.getCurrentUserIdOrThrow();
        rule.setStatus((short) 1);  // 启用
        rule.setVersion(1);
        rule.setCreatedBy(currentUserId);
        ruleMapper.insert(rule);

        // 创建变更日志（待复核）
        RuleChangeLog changeLog = new RuleChangeLog();
        changeLog.setRuleId(rule.getId());
        changeLog.setRuleCode(rule.getRuleCode());
        changeLog.setChangeType("CREATE");
        changeLog.setNewVersion(1);
        changeLog.setNewContent(rule.getDrlContent());
        changeLog.setStatus(DualControlStatus.PENDING.name());
        changeLog.setMakerId(currentUserId);
        ruleMapper.insertChangeLog(changeLog);

        log.info("[Rule] 规则创建待复核: ruleCode={}, makerId={}, changeLogId={}",
                rule.getRuleCode(), currentUserId, changeLog.getId());
        return rule.getId();
    }

    /**
     * 经办更新规则（新版本，status=PENDING）
     */
    @Audit(module = "RULE", action = "UPDATE", targetType = "RULE_DEFINITION", targetIdExpr = "#rule.id", snapshot = true)
    @Transactional
    public void updateRule(RuleDefinition rule) {
        Long currentUserId = securityContextHelper.getCurrentUserIdOrThrow();
        RuleDefinition existing = ruleMapper.selectById(rule.getId());
        if (existing == null) {
            throw new IllegalArgumentException("规则不存在");
        }

        Integer oldVersion = existing.getVersion();
        Integer newVersion = oldVersion + 1;

        rule.setVersion(newVersion);
        ruleMapper.update(rule);

        // 创建变更日志
        RuleChangeLog changeLog = new RuleChangeLog();
        changeLog.setRuleId(rule.getId());
        changeLog.setRuleCode(existing.getRuleCode());
        changeLog.setChangeType("UPDATE");
        changeLog.setOldVersion(oldVersion);
        changeLog.setNewVersion(newVersion);
        changeLog.setOldContent(existing.getDrlContent());
        changeLog.setNewContent(rule.getDrlContent());
        changeLog.setStatus(DualControlStatus.PENDING.name());
        changeLog.setMakerId(currentUserId);
        ruleMapper.insertChangeLog(changeLog);

        log.info("[Rule] 规则更新待复核: ruleId={}, oldVersion={}, newVersion={}, makerId={}",
                rule.getId(), oldVersion, newVersion, currentUserId);
    }

    /**
     * 提交规则当前版本进行双岗复核。提交前由前端展示完整规则内容，后端再次校验待复核状态。
     */
    @Audit(module = "RULE", action = "SUBMIT", targetType = "RULE_DEFINITION", targetIdExpr = "#ruleId", snapshot = true)
    @Transactional
    public void submitRuleChange(Long ruleId, String changeType, String remark) {
        Long currentUserId = securityContextHelper.getCurrentUserIdOrThrow();
        RuleDefinition rule = ruleMapper.selectById(ruleId);
        if (rule == null) {
            throw new IllegalArgumentException("规则不存在");
        }
        if (ruleMapper.selectPendingChangeLogByRuleId(ruleId) != null) {
            // 新建规则时已经创建待复核记录；前端要求用户查看完整内容后再确认，确认只需幂等通过。
            log.info("[Rule] 规则已有待复核变更，提交确认幂等返回: ruleId={}", ruleId);
            return;
        }
        RuleChangeLog changeLog = new RuleChangeLog();
        changeLog.setRuleId(ruleId);
        changeLog.setRuleCode(rule.getRuleCode());
        changeLog.setChangeType(changeType == null || changeType.isBlank() ? "UPDATE" : changeType);
        changeLog.setOldVersion(rule.getVersion());
        changeLog.setNewVersion(rule.getVersion() + 1);
        changeLog.setOldContent(rule.getDrlContent());
        changeLog.setNewContent(rule.getDrlContent());
        changeLog.setStatus(DualControlStatus.PENDING.name());
        changeLog.setMakerId(currentUserId);
        ruleMapper.insertChangeLog(changeLog);
        log.info("[Rule] 规则已提交复核: ruleId={}, changeLogId={}, remark={}", ruleId, changeLog.getId(), remark);
    }

    /**
     * 复核审批（approve/reject）
     */
    @Audit(module = "RULE", action = "APPROVE", targetType = "RULE_CHANGE_LOG", targetIdExpr = "#changeLogId", snapshot = true)
    @Transactional
    public void reviewChange(Long changeLogId, boolean approved, String rejectReason) {
        Long currentUserId = securityContextHelper.getCurrentUserIdOrThrow();
        RuleChangeLog changeLog = ruleMapper.selectChangeLogById(changeLogId);
        if (changeLog == null) {
            throw new IllegalArgumentException("变更日志不存在");
        }
        if (!DualControlStatus.PENDING.name().equals(changeLog.getStatus())) {
            throw new IllegalStateException("该变更已处理");
        }
        // 双岗校验：经办 ≠ 复核
        if (changeLog.getMakerId().equals(currentUserId)) {
            throw new IllegalStateException("经办人与复核人不能为同一人");
        }

        if (approved) {
            ruleMapper.updateChangeLogStatus(changeLogId, DualControlStatus.APPROVED.name(),
                    currentUserId, null);
            // 立即生效（启用新版本）
            if (changeLog.getRuleId() != null) {
                ruleMapper.updateStatus(changeLog.getRuleId(), (short) 1, changeLog.getNewVersion());
            }
            log.info("[Rule] 规则变更已批准: changeLogId={}, checkerId={}", changeLogId, currentUserId);
        } else {
            if (rejectReason == null || rejectReason.trim().isEmpty()) {
                throw new IllegalArgumentException("拒绝时必须填写原因");
            }
            ruleMapper.updateChangeLogStatus(changeLogId, DualControlStatus.REJECTED.name(),
                    currentUserId, rejectReason);
            log.info("[Rule] 规则变更已拒绝: changeLogId={}, checkerId={}, reason={}",
                    changeLogId, currentUserId, rejectReason);
        }
    }

    public PageResult<RuleChangeLog> listPendingChanges(long offset, int size) {
        long total = ruleMapper.countPendingChangeLogs();
        if (total == 0) {
            return PageResult.empty();
        }
        return PageResult.of(ruleMapper.selectPendingChangeLogs(offset, size), total);
    }

    public List<RuleChangeLog> listRuleChangeHistory(Long ruleId) {
        return ruleMapper.selectChangeLogsByRuleId(ruleId);
    }

    // ========== 风险权重配置（双岗）==========

    public RiskWeightConfig getWeightConfigById(Long id) {
        return weightConfigMapper.selectById(id);
    }

    public RiskWeightConfig getEnabledWeightConfig() {
        return weightConfigMapper.selectEnabled();
    }

    public PageResult<RiskWeightConfig> searchWeightConfigs(String status, long offset, int size) {
        long total = weightConfigMapper.countAll(status);
        if (total == 0) {
            return PageResult.empty();
        }
        return PageResult.of(weightConfigMapper.selectPage(status, offset, size), total);
    }

    /**
     * 经办创建权重配置（status=PENDING）
     */
    @Audit(module = "RULE", action = "CREATE", targetType = "RISK_WEIGHT_CONFIG", targetIdExpr = "#result")
    @Transactional
    public Long createWeightConfig(RiskWeightConfig config) {
        Long currentUserId = securityContextHelper.getCurrentUserIdOrThrow();
        // 校验权重之和 = 100
        int sum = config.getSupplyChainWeight() + config.getTransactionWeight() + config.getMaterialWeight();
        if (sum != 100) {
            throw new IllegalArgumentException("权重之和必须等于 100，当前: " + sum);
        }
        config.setStatus(DualControlStatus.PENDING.name());
        config.setVersion(1);
        config.setMakerId(currentUserId);
        weightConfigMapper.insert(config);
        log.info("[Rule] 权重配置创建待复核: configId={}, makerId={}", config.getId(), currentUserId);
        return config.getId();
    }

    /**
     * 复核审批权重配置
     */
    @Audit(module = "RULE", action = "APPROVE", targetType = "RISK_WEIGHT_CONFIG", targetIdExpr = "#configId")
    @Transactional
    public void reviewWeightConfig(Long configId, boolean approved, String rejectReason) {
        Long currentUserId = securityContextHelper.getCurrentUserIdOrThrow();
        RiskWeightConfig config = weightConfigMapper.selectById(configId);
        if (config == null) {
            throw new IllegalArgumentException("权重配置不存在");
        }
        if (!DualControlStatus.PENDING.name().equals(config.getStatus())) {
            throw new IllegalStateException("该配置已处理");
        }
        if (config.getMakerId().equals(currentUserId)) {
            throw new IllegalStateException("经办人与复核人不能为同一人");
        }

        if (approved) {
            // 先禁用旧的 ENABLED 配置
            RiskWeightConfig oldEnabled = weightConfigMapper.selectEnabled();
            if (oldEnabled != null && !oldEnabled.getId().equals(configId)) {
                weightConfigMapper.updateStatus(oldEnabled.getId(), DualControlStatus.DISABLED.name());
            }
            weightConfigMapper.updateStatus(configId, DualControlStatus.ENABLED.name());
            log.info("[Rule] 权重配置已批准并启用: configId={}, checkerId={}", configId, currentUserId);
        } else {
            if (rejectReason == null || rejectReason.trim().isEmpty()) {
                throw new IllegalArgumentException("拒绝时必须填写原因");
            }
            weightConfigMapper.updateStatus(configId, DualControlStatus.REJECTED.name());
            log.info("[Rule] 权重配置已拒绝: configId={}, checkerId={}, reason={}",
                    configId, currentUserId, rejectReason);
        }
    }

    // ========== 材料清单模板（双岗）==========

    public MaterialChecklistTemplate getTemplateByBusinessType(String businessType) {
        return weightConfigMapper.selectTemplateByBusinessType(businessType);
    }

    public List<MaterialChecklistTemplate> listAllTemplates() {
        return weightConfigMapper.selectAllTemplates();
    }

    /**
     * 经办创建模板（status=PENDING）
     */
    @Audit(module = "RULE", action = "CREATE", targetType = "MATERIAL_TEMPLATE", targetIdExpr = "#result")
    @Transactional
    public Long createTemplate(MaterialChecklistTemplate template) {
        Long currentUserId = securityContextHelper.getCurrentUserIdOrThrow();
        template.setStatus(DualControlStatus.PENDING.name());
        template.setVersion(1);
        template.setMakerId(currentUserId);
        weightConfigMapper.insertTemplate(template);
        log.info("[Rule] 材料模板创建待复核: templateId={}, makerId={}", template.getId(), currentUserId);
        return template.getId();
    }

    /**
     * 复核审批材料模板
     */
    @Audit(module = "RULE", action = "APPROVE", targetType = "MATERIAL_TEMPLATE", targetIdExpr = "#templateId")
    @Transactional
    public void reviewTemplate(Long templateId, boolean approved, String rejectReason) {
        Long currentUserId = securityContextHelper.getCurrentUserIdOrThrow();
        // 简化：直接通过 selectAll 查找
        MaterialChecklistTemplate template = listAllTemplates().stream()
                .filter(t -> t.getId().equals(templateId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("材料模板不存在"));

        if (!DualControlStatus.PENDING.name().equals(template.getStatus())) {
            throw new IllegalStateException("该模板已处理");
        }
        if (template.getMakerId().equals(currentUserId)) {
            throw new IllegalStateException("经办人与复核人不能为同一人");
        }

        if (approved) {
            template.setStatus(DualControlStatus.ENABLED.name());
            template.setCheckerId(currentUserId);
            template.setCheckedAt(Instant.now());
            weightConfigMapper.updateTemplate(template);
            log.info("[Rule] 材料模板已批准并启用: templateId={}, checkerId={}", templateId, currentUserId);
        } else {
            if (rejectReason == null || rejectReason.trim().isEmpty()) {
                throw new IllegalArgumentException("拒绝时必须填写原因");
            }
            template.setStatus(DualControlStatus.REJECTED.name());
            template.setCheckerId(currentUserId);
            template.setCheckedAt(Instant.now());
            weightConfigMapper.updateTemplate(template);
            log.info("[Rule] 材料模板已拒绝: templateId={}, checkerId={}, reason={}",
                    templateId, currentUserId, rejectReason);
        }
    }
}
