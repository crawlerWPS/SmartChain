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
@RequestMapping("/api")
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
        return Result.ok(ruleService.searchRules(cat, status, keyword, pageQuery.offset(), pageQuery.getSize()));
    }

    @RequirePermission(module = "RULE", permission = "view")
    @GetMapping("/rules/{id}")
    public Result<RuleDefinition> getRule(@PathVariable Long id) {
        return Result.ok(ruleService.getRuleById(id));
    }

    @RequirePermission(module = "RULE", permission = "create")
    @PostMapping("/rules")
    public Result<Long> createRule(@RequestBody RuleDefinition rule) {
        return Result.ok(ruleService.createRule(rule));
    }

    @RequirePermission(module = "RULE", permission = "update")
    @PutMapping("/rules/{id}")
    public Result<Void> updateRule(@PathVariable Long id, @RequestBody RuleDefinition rule) {
        rule.setId(id);
        ruleService.updateRule(rule);
        return Result.ok();
    }

    // ========== 变更日志（双岗审批）==========
    @RequirePermission(module = "RULE", permission = "view")
    @GetMapping("/rule-changes/pending")
    public Result<PageResult<RuleChangeLog>> listPendingChanges(PageQuery pageQuery) {
        return Result.ok(ruleService.listPendingChanges(pageQuery.offset(), pageQuery.getSize()));
    }

    @RequirePermission(module = "RULE", permission = "approve")
    @PostMapping("/rule-changes/{id}/review")
    public Result<Void> reviewChange(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        boolean approved = (boolean) body.get("approved");
        String rejectReason = (String) body.get("rejectReason");
        ruleService.reviewChange(id, approved, rejectReason);
        return Result.ok();
    }

    @RequirePermission(module = "RULE", permission = "view")
    @GetMapping("/rules/{id}/changes")
    public Result<List<RuleChangeLog>> listRuleChanges(@PathVariable Long id) {
        return Result.ok(ruleService.listRuleChangeHistory(id));
    }

    // ========== 风险权重配置 ==========
    @RequirePermission(module = "RULE", permission = "view")
    @GetMapping("/weights")
    public Result<PageResult<RiskWeightConfig>> listWeights(PageQuery pageQuery,
                                                              @RequestParam(required = false) String status) {
        return Result.ok(ruleService.searchWeightConfigs(status, pageQuery.offset(), pageQuery.getSize()));
    }

    @RequirePermission(module = "RULE", permission = "view")
    @GetMapping("/weights/enabled")
    public Result<RiskWeightConfig> getEnabledWeight() {
        return Result.ok(ruleService.getEnabledWeightConfig());
    }

    @RequirePermission(module = "RULE", permission = "create")
    @PostMapping("/weights")
    public Result<Long> createWeight(@RequestBody RiskWeightConfig config) {
        return Result.ok(ruleService.createWeightConfig(config));
    }

    @RequirePermission(module = "RULE", permission = "approve")
    @PostMapping("/weights/{id}/review")
    public Result<Void> reviewWeight(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        boolean approved = (boolean) body.get("approved");
        String rejectReason = (String) body.get("rejectReason");
        ruleService.reviewWeightConfig(id, approved, rejectReason);
        return Result.ok();
    }

    // ========== 材料清单模板 ==========
    @RequirePermission(module = "RULE", permission = "view")
    @GetMapping("/templates")
    public Result<List<MaterialChecklistTemplate>> listTemplates() {
        return Result.ok(ruleService.listAllTemplates());
    }

    @RequirePermission(module = "RULE", permission = "create")
    @PostMapping("/templates")
    public Result<Long> createTemplate(@RequestBody MaterialChecklistTemplate template) {
        return Result.ok(ruleService.createTemplate(template));
    }

    @RequirePermission(module = "RULE", permission = "approve")
    @PostMapping("/templates/{id}/review")
    public Result<Void> reviewTemplate(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        boolean approved = (boolean) body.get("approved");
        String rejectReason = (String) body.get("rejectReason");
        ruleService.reviewTemplate(id, approved, rejectReason);
        return Result.ok();
    }
}
