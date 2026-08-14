package com.scfs.common.controller;

import com.scfs.common.core.PageQuery;
import com.scfs.common.core.PageResult;
import com.scfs.common.core.Result;
import com.scfs.common.entity.MaterialChecklistTemplate;
import com.scfs.common.entity.RuleChangeLog;
import com.scfs.common.entity.RuleDefinition;
import com.scfs.common.entity.RiskWeightConfig;
import com.scfs.common.enums.RuleCategory;
import com.scfs.common.security.RequirePermission;
import com.scfs.common.service.RuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 规则管理 Controller - 对应 RFC 3.x /api/rules /api/weights /api/templates
 *
 * <p>双岗审批流程：OPS_MAKER 经办 → OPS_CHECKER 复核</p>
 */
@RestController
@RequestMapping
@RequiredArgsConstructor
public class RuleController {

    private final RuleService ruleService;

    // ========== 规则定义 ==========
    @RequirePermission(module = "RULE", permission = "view")
    @GetMapping("/rules")
    public Result<PageResult<RuleDefinition>> listRules(PageQuery pageQuery,
                                                          @RequestParam(required = false) String category,
                                                          @RequestParam(required = false) Short status,
                                                          @RequestParam(required = false) String keyword) {
        RuleCategory cat = category == null ? null : RuleCategory.valueOf(category);
        return Result.success(ruleService.searchRules(cat, status, keyword, pageQuery.offset(), pageQuery.getSize()));
    }

    @RequirePermission(module = "RULE", permission = "view")
    @GetMapping("/rules/{id}")
    public Result<RuleDefinition> getRule(@PathVariable Long id) {
        return Result.success(ruleService.getRuleById(id));
    }

    @RequirePermission(module = "RULE", permission = "create")
    @PostMapping("/rules")
    public Result<Long> createRule(@RequestBody RuleDefinition rule) {
        return Result.success(ruleService.createRule(rule));
    }

    @RequirePermission(module = "RULE", permission = "update")
    @PutMapping("/rules/{id}")
    public Result<Void> updateRule(@PathVariable Long id, @RequestBody RuleDefinition rule) {
        rule.setId(id);
        ruleService.updateRule(rule);
        return Result.success();
    }

    @RequirePermission(module = "RULE", permission = "create")
    @PostMapping("/rules/{id}/submit")
    public Result<Void> submitRule(@PathVariable Long id,
                                   @RequestParam(required = false, defaultValue = "UPDATE") String changeType,
                                   @RequestBody(required = false) Map<String, Object> body) {
        String remark = body == null ? null : (String) body.get("remark");
        ruleService.submitRuleChange(id, changeType, remark);
        return Result.success();
    }

    @RequirePermission(module = "RULE", permission = "update")
    @PatchMapping("/rules/{id}/status")
    public Result<Void> updateRuleStatus(@PathVariable Long id, @RequestParam Short status) {
        ruleService.updateRuleStatus(id, status);
        return Result.success();
    }

    // ========== 变更日志（双岗审批）==========
    @RequirePermission(module = "RULE", permission = "view")
    @GetMapping("/rule-changes/pending")
    public Result<PageResult<RuleChangeLog>> listPendingChanges(PageQuery pageQuery) {
        return Result.success(ruleService.listPendingChanges(pageQuery.offset(), pageQuery.getSize()));
    }

    @RequirePermission(module = "RULE", permission = "approve")
    @PostMapping("/rule-changes/{id}/review")
    public Result<Void> reviewChange(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        boolean approved = (boolean) body.get("approved");
        String rejectReason = (String) body.get("rejectReason");
        ruleService.reviewChange(id, approved, rejectReason);
        return Result.success();
    }

    @RequirePermission(module = "RULE", permission = "view")
    @GetMapping("/rules/{id}/changes")
    public Result<List<RuleChangeLog>> listRuleChanges(@PathVariable Long id) {
        return Result.success(ruleService.listRuleChangeHistory(id));
    }

    // ========== 风险权重配置 ==========
    @RequirePermission(module = "RULE", permission = "view")
    @GetMapping("/weights")
    public Result<PageResult<RiskWeightConfig>> listWeights(PageQuery pageQuery,
                                                              @RequestParam(required = false) String status) {
        return Result.success(ruleService.searchWeightConfigs(status, pageQuery.offset(), pageQuery.getSize()));
    }

    @RequirePermission(module = "RULE", permission = "view")
    @GetMapping("/weights/enabled")
    public Result<RiskWeightConfig> getEnabledWeight() {
        return Result.success(ruleService.getEnabledWeightConfig());
    }

    @RequirePermission(module = "RULE", permission = "create")
    @PostMapping("/weights")
    public Result<Long> createWeight(@RequestBody RiskWeightConfig config) {
        return Result.success(ruleService.createWeightConfig(config));
    }

    @RequirePermission(module = "RULE", permission = "approve")
    @PostMapping("/weights/{id}/review")
    public Result<Void> reviewWeight(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        boolean approved = (boolean) body.get("approved");
        String rejectReason = (String) body.get("rejectReason");
        ruleService.reviewWeightConfig(id, approved, rejectReason);
        return Result.success();
    }

    // ========== 材料清单模板 ==========
    @RequirePermission(module = "RULE", permission = "view")
    @GetMapping("/templates")
    public Result<List<MaterialChecklistTemplate>> listTemplates() {
        return Result.success(ruleService.listAllTemplates());
    }

    @RequirePermission(module = "RULE", permission = "create")
    @PostMapping("/templates")
    public Result<Long> createTemplate(@RequestBody MaterialChecklistTemplate template) {
        return Result.success(ruleService.createTemplate(template));
    }

    @RequirePermission(module = "RULE", permission = "update")
    @PutMapping("/templates/{id}")
    public Result<Void> updateTemplate(@PathVariable Long id, @RequestBody MaterialChecklistTemplate template) {
        ruleService.updateTemplate(id, template);
        return Result.success();
    }

    @RequirePermission(module = "RULE", permission = "delete")
    @DeleteMapping("/templates/{id}")
    public Result<Void> deleteTemplate(@PathVariable Long id) {
        ruleService.deleteTemplate(id);
        return Result.success();
    }

}
