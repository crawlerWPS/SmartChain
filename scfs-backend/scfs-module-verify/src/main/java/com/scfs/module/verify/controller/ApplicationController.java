package com.scfs.module.verify.controller;

import com.scfs.common.core.PageQuery;
import com.scfs.common.core.PageResult;
import com.scfs.common.core.Result;
import com.scfs.common.security.RequirePermission;
import com.scfs.module.verify.entity.ApplicationStatusHistory;
import com.scfs.module.verify.entity.ApplicationCustomer;
import com.scfs.module.verify.entity.FinancingApplication;
import com.scfs.module.verify.entity.MaterialRecognitionResult;
import com.scfs.module.verify.entity.VerifyCheckResult;
import com.scfs.module.verify.entity.VerifyReport;
import com.scfs.module.verify.entity.OcrRecognitionTemplate;
import com.scfs.module.verify.service.ApplicationMaterialService;
import com.scfs.module.verify.service.FinancingApplicationService;
import com.scfs.module.verify.service.VerifyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 融资申请 Controller - 对应 RFC 3.x /api/applications
 */
@RestController
@RequestMapping("/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final FinancingApplicationService applicationService;
    private final ApplicationMaterialService materialService;
    private final VerifyService verifyService;


    @RequirePermission(module = "VERIFY", permission = "view")
    @GetMapping
    public Result<PageResult<FinancingApplication>> list(PageQuery pageQuery,
                                                           @RequestParam(required = false) String status,
                                                           @RequestParam(required = false) Long submittedBy,
                                                           @RequestParam(required = false) Long enterpriseId,
                                                           @RequestParam(required = false) String keyword) {
        return Result.success(applicationService.search(status, submittedBy, enterpriseId, keyword,
                pageQuery.offset(), pageQuery.getSize()));
    }

    @RequirePermission(module = "VERIFY", permission = "view")
    @GetMapping("/customers")
    public Result<List<ApplicationCustomer>> customers(@RequestParam(required = false) String keyword,
                                                       @RequestParam(required = false, defaultValue = "false") boolean buyerOnly) {
        return Result.success(buyerOnly
                ? applicationService.searchBuyerCustomers(keyword)
                : applicationService.searchCustomers(keyword));
    }

    @RequirePermission(module = "VERIFY", permission = "view")
    @GetMapping("/customers/sellers")
    public Result<List<ApplicationCustomer>> sellers(@RequestParam Long buyerEnterpriseId,
                                                     @RequestParam(required = false) String keyword) {
        return Result.success(applicationService.searchSellerCustomers(buyerEnterpriseId, keyword));
    }

    @RequirePermission(module = "VERIFY", permission = "create")
    @PostMapping("/customers")
    public Result<Long> createCustomer(@RequestBody ApplicationCustomer customer) {
        return Result.success(applicationService.saveCustomer(customer));
    }

    @RequirePermission(module = "VERIFY", permission = "update")
    @PutMapping("/customers/{id}")
    public Result<Long> updateCustomer(@PathVariable Long id, @RequestBody ApplicationCustomer customer) {
        customer.setEnterpriseId(id);
        return Result.success(applicationService.saveCustomer(customer));
    }

    @RequirePermission(module = "VERIFY", permission = "create")
    @PostMapping("/trade-relations")
    public Result<Void> tradeRelation(@RequestBody Map<String, Long> body) {
        applicationService.maintainTradeRelation(body.get("buyerEnterpriseId"), body.get("sellerEnterpriseId"));
        return Result.success();
    }

    @RequirePermission(module = "VERIFY", permission = "view")
    @GetMapping("/{id}")
    public Result<FinancingApplication> get(@PathVariable Long id) {
        return Result.success(applicationService.getById(id));
    }

    @RequirePermission(module = "VERIFY", permission = "create")
    @PostMapping
    public Result<Long> create(@RequestBody FinancingApplication application) {
        return Result.success(applicationService.createApplication(application));
    }

    @RequirePermission(module = "VERIFY", permission = "update")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody FinancingApplication application) {
        application.setId(id);
        applicationService.updateApplication(application);
        return Result.success();
    }

    @RequirePermission(module = "VERIFY", permission = "update")
    @PostMapping("/{id}/submit")
    public Result<Void> submit(@PathVariable Long id) {
        applicationService.submit(id);
        return Result.success();
    }

    @RequirePermission(module = "VERIFY", permission = "approve")
    @PostMapping("/{id}/assign")
    public Result<Void> assign(@PathVariable Long id, @RequestParam Long handlerId) {
        applicationService.assignHandler(id, handlerId);
        return Result.success();
    }

    @RequirePermission(module = "VERIFY", permission = "update")
    @PostMapping("/{id}/move-to-pre-audit")
    public Result<Void> moveToPreAudit(@PathVariable Long id) {
        applicationService.moveToPreAudit(id);
        return Result.success();
    }

    @RequirePermission(module = "VERIFY", permission = "update")
    @PostMapping("/{id}/move-to-verify")
    public Result<Void> moveToVerify(@PathVariable Long id) {
        applicationService.moveToVerify(id);
        return Result.success();
    }

    @RequirePermission(module = "VERIFY", permission = "approve")
    @PostMapping("/{id}/pre-audit-passed")
    public Result<Void> preAuditPassed(@PathVariable Long id) {
        applicationService.preAuditPassed(id);
        return Result.success();
    }

    @RequirePermission(module = "VERIFY", permission = "reject")
    @PostMapping("/{id}/pre-audit-failed")
    public Result<Void> preAuditFailed(@PathVariable Long id, @RequestBody Map<String, String> body) {
        applicationService.preAuditFailed(id, body.get("reason"));
        return Result.success();
    }

    @RequirePermission(module = "VERIFY", permission = "update")
    @PostMapping("/{id}/move-to-risk-scoring")
    public Result<Void> moveToRiskScoring(@PathVariable Long id) {
        applicationService.moveToRiskScoring(id);
        return Result.success();
    }

    @RequirePermission(module = "VERIFY", permission = "update")
    @PostMapping("/{id}/move-to-pending-decision")
    public Result<Void> moveToPendingDecision(@PathVariable Long id) {
        applicationService.moveToPendingDecision(id);
        return Result.success();
    }

    @RequirePermission(module = "VERIFY", permission = "approve")
    @PostMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable Long id, @RequestBody Map<String, String> body) {
        applicationService.approve(id, body.get("remark"));
        return Result.success();
    }

    @RequirePermission(module = "VERIFY", permission = "reject")
    @PostMapping("/{id}/reject")
    public Result<Void> reject(@PathVariable Long id, @RequestBody Map<String, String> body) {
        applicationService.reject(id, body.get("remark"));
        return Result.success();
    }

    @RequirePermission(module = "VERIFY", permission = "approve")
    @PostMapping("/{id}/revoke")
    public Result<Void> revoke(@PathVariable Long id, @RequestBody Map<String, String> body) {
        applicationService.revokeApproval(id, body.get("reason"));
        return Result.success();
    }

    @RequirePermission(module = "VERIFY", permission = "view")
    @GetMapping("/{id}/status-history")
    public Result<List<ApplicationStatusHistory>> statusHistory(@PathVariable Long id) {
        return Result.success(applicationService.getStatusHistory(id));
    }

    // ========== 材料 ==========
    @RequirePermission(module = "VERIFY", permission = "view")
    @GetMapping("/{id}/materials")
    public Result<?> listMaterials(@PathVariable Long id) {
        return Result.success(materialService.listByApplication(id));
    }

    @RequirePermission(module = "VERIFY", permission = "create")
    @PostMapping("/{id}/materials")
    public Result<Long> uploadMaterial(@PathVariable Long id,
                                        @RequestParam("file") MultipartFile file,
                                        @RequestParam String materialType,
                                        @RequestParam(required = false) Long ocrTemplateId) {
        return Result.success(materialService.uploadMaterial(id, file, materialType, ocrTemplateId));
    }

    @RequirePermission(module = "VERIFY", permission = "create")
    @GetMapping("/materials/ocr-templates")
    public Result<List<OcrRecognitionTemplate>> selectableOcrTemplates(@RequestParam String materialType) {
        return Result.success(materialService.listSelectableOcrTemplates(materialType));
    }

    @RequirePermission(module = "VERIFY", permission = "update")
    @PutMapping("/materials/{id}/type")
    public Result<Void> updateMaterialType(@PathVariable Long id, @RequestBody Map<String, String> body) {
        materialService.updateMaterialType(id, body.get("materialType"));
        return Result.success();
    }

    @RequirePermission(module = "VERIFY", permission = "view")
    @GetMapping("/materials/{id}/recognition")
    public Result<MaterialRecognitionResult> getRecognition(@PathVariable Long id) {
        return Result.success(materialService.getRecognitionResult(id));
    }

    @RequirePermission(module = "VERIFY", permission = "update")
    @PostMapping("/materials/{id}/re-recognize")
    public Result<Void> reRecognize(@PathVariable Long id) {
        materialService.reRecognize(id);
        return Result.success();
    }

    @RequirePermission(module = "VERIFY", permission = "update")
    @PutMapping("/materials/{id}/recognition")
    public Result<Void> updateRecognition(@PathVariable Long id, @RequestBody MaterialRecognitionResult result) {
        materialService.updateRecognitionResult(id, result);
        return Result.success();
    }

    @RequirePermission(module = "VERIFY", permission = "delete")
    @DeleteMapping("/materials/{id}")
    public Result<Void> deleteMaterial(@PathVariable Long id) {
        materialService.deleteMaterial(id);
        return Result.success();
    }

    // ========== 核验 ==========
    @RequirePermission(module = "VERIFY", permission = "view")
    @GetMapping("/{id}/verify-results")
    public Result<List<VerifyCheckResult>> getVerifyResults(@PathVariable Long id) {
        return Result.success(verifyService.getCheckResults(id));
    }

    @RequirePermission(module = "VERIFY", permission = "update")
    @PostMapping("/{id}/verify")
    public Result<List<VerifyCheckResult>> verify(@PathVariable Long id) {
        return Result.success(verifyService.verifyAll(id));
    }

    @RequirePermission(module = "VERIFY", permission = "update")
    @PostMapping("/{id}/verify-report")
    public Result<VerifyReport> generateReport(@PathVariable Long id) {
        return Result.success(verifyService.generateReport(id));
    }

    @RequirePermission(module = "VERIFY", permission = "view")
    @GetMapping("/{id}/verify-report")
    public Result<VerifyReport> getReport(@PathVariable Long id) {
        return Result.success(verifyService.getReportByApplication(id));
    }
}
