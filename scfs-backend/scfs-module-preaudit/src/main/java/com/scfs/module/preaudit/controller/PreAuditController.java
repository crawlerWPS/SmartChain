package com.scfs.module.preaudit.controller;

import com.scfs.common.core.Result;
import com.scfs.common.security.RequirePermission;
import com.scfs.module.preaudit.entity.*;
import com.scfs.module.preaudit.service.PreAuditService;
import com.scfs.module.verify.entity.FinancingApplication;
import com.scfs.module.verify.mapper.VerifyMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 预审 Controller - 对应 RFC 3.x /api/applications/{id}/pre-audit/*
 */
@RestController
@RequestMapping("/applications/{applicationId}/pre-audit")
@RequiredArgsConstructor
public class PreAuditController {

    private final PreAuditService preAuditService;
    private final VerifyMapper verifyMapper;

    // ========== 完整性 ==========
    @RequirePermission(module = "PREAUDIT", permission = "view")
    @GetMapping("/completeness")
    public Result<MaterialCompletenessResult> getCompleteness(@PathVariable Long applicationId) {
        return Result.success(preAuditService.getCompleteness(applicationId));
    }

    @RequirePermission(module = "PREAUDIT", permission = "update")
    @PostMapping("/completeness/check")
    public Result<MaterialCompletenessResult> checkCompleteness(@PathVariable Long applicationId) {
        FinancingApplication app = verifyMapper.selectApplicationById(applicationId);
        return Result.success(preAuditService.checkCompleteness(applicationId, app.getBusinessType()));
    }

    // ========== 有效性 ==========
    @RequirePermission(module = "PREAUDIT", permission = "view")
    @GetMapping("/validity")
    public Result<MaterialValidityResult> getValidity(@PathVariable Long applicationId) {
        return Result.success(preAuditService.getValidity(applicationId));
    }

    @RequirePermission(module = "PREAUDIT", permission = "update")
    @PostMapping("/validity/check")
    public Result<MaterialValidityResult> checkValidity(@PathVariable Long applicationId) {
        return Result.success(preAuditService.checkValidity(applicationId));
    }

    // ========== 一致性 ==========
    @RequirePermission(module = "PREAUDIT", permission = "view")
    @GetMapping("/consistency")
    public Result<EnterpriseInfoConsistencyResult> getConsistency(@PathVariable Long applicationId) {
        return Result.success(preAuditService.getConsistency(applicationId));
    }

    @RequirePermission(module = "PREAUDIT", permission = "update")
    @PostMapping("/consistency/check")
    public Result<EnterpriseInfoConsistencyResult> checkConsistency(@PathVariable Long applicationId) {
        return Result.success(preAuditService.checkConsistency(applicationId));
    }

    @RequirePermission(module = "PREAUDIT", permission = "view")
    @GetMapping("/consistency/{resultId}/mismatches")
    public Result<List<EnterpriseInfoMismatchDetail>> getMismatches(@PathVariable Long resultId) {
        return Result.success(preAuditService.getMismatchDetails(resultId));
    }

    // ========== 补正清单 ==========
    @RequirePermission(module = "PREAUDIT", permission = "view")
    @GetMapping("/supplement-list")
    public Result<SupplementList> getSupplementList(@PathVariable Long applicationId) {
        return Result.success(preAuditService.getSupplementList(applicationId));
    }

    @RequirePermission(module = "PREAUDIT", permission = "update")
    @PostMapping("/supplement-list/generate")
    public Result<SupplementList> generateSupplementList(@PathVariable Long applicationId) {
        return Result.success(preAuditService.generateSupplementList(applicationId));
    }

    @RequirePermission(module = "PREAUDIT", permission = "update")
    @PostMapping("/supplement-list/complete")
    public Result<Void> markSupplementCompleted(@PathVariable Long applicationId) {
        preAuditService.markSupplementCompleted(applicationId);
        return Result.success();
    }
}
