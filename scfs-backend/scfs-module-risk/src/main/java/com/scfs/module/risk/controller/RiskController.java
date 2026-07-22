package com.scfs.module.risk.controller;

import com.scfs.common.core.Result;
import com.scfs.common.security.RequirePermission;
import com.scfs.module.risk.entity.RiskProfile;
import com.scfs.module.risk.service.RiskService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 风险评分 Controller - 对应 RFC 3.x /api/applications/{id}/risk
 */
@RestController
@RequestMapping("/api/applications/{applicationId}/risk")
@RequiredArgsConstructor
public class RiskController {

    private final RiskService riskService;

    @RequirePermission(module = "RISK", permission = "view")
    @GetMapping
    public Result<RiskProfile> get(@PathVariable Long applicationId) {
        return Result.ok(riskService.getProfileByApplication(applicationId));
    }

    @RequirePermission(module = "RISK", permission = "update")
    @PostMapping("/calculate")
    public Result<RiskProfile> calculate(@PathVariable Long applicationId) {
        return Result.ok(riskService.calculate(applicationId));
    }

    @RequirePermission(module = "RISK", permission = "view")
    @GetMapping("/enterprises/{enterpriseId}")
    public Result<List<RiskProfile>> getByEnterprise(@PathVariable Long enterpriseId) {
        return Result.ok(riskService.getProfilesByEnterprise(enterpriseId));
    }
}
