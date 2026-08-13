package com.scfs.module.graph.controller;

import com.scfs.common.core.PageQuery;
import com.scfs.common.core.PageResult;
import com.scfs.common.core.Result;
import com.scfs.common.security.RequirePermission;
import com.scfs.module.graph.entity.AbnormalRelation;
import com.scfs.module.graph.entity.Enterprise;
import com.scfs.module.graph.entity.EnterprisePositionAnalysis;
import com.scfs.module.graph.entity.EnterpriseRole;
import com.scfs.module.graph.entity.RelationImportResult;
import com.scfs.module.graph.entity.RelationImportRow;
import com.scfs.module.graph.service.GraphService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 图谱 Controller - 对应 RFC 3.x /api/graph
 */
@RestController
@RequestMapping("/graph")
@RequiredArgsConstructor
public class GraphController {

    private final GraphService graphService;

    // ========== 企业 ==========
    @RequirePermission(module = "GRAPH", permission = "view")
    @GetMapping("/enterprises")
    public Result<PageResult<Enterprise>> searchEnterprises(PageQuery pageQuery,
                                                               @RequestParam(required = false) String keyword) {
        return Result.success(graphService.searchEnterprises(keyword, pageQuery.offset(), pageQuery.getSize()));
    }

    @RequirePermission(module = "GRAPH", permission = "view")
    @GetMapping("/enterprises/{id}")
    public Result<Enterprise> getEnterprise(@PathVariable Long id) {
        return Result.success(graphService.getEnterpriseById(id));
    }

    // ========== 关系图谱 ==========
    @RequirePermission(module = "GRAPH", permission = "view")
    @GetMapping("/relations/{enterpriseId}")
    public Result<Map<String, Object>> getRelationGraph(@PathVariable Long enterpriseId,
                                                          @RequestParam(defaultValue = "1") int level) {
        return Result.success(graphService.getRelationGraph(enterpriseId, level));
    }

    @RequirePermission(module = "GRAPH", permission = "view")
    @GetMapping("/relations")
    public Result<List<?>> getAllRelations() {
        return Result.success(graphService.getAllRelations());
    }

    @RequirePermission(module = "GRAPH", permission = "view")
    @GetMapping("/full")
    public Result<Map<String, Object>> getAllRelationGraph() {
        return Result.success(graphService.getAllRelationGraph());
    }

    @RequirePermission(module = "GRAPH", permission = "update")
    @PostMapping("/relations/import")
    public Result<RelationImportResult> importRelations(@RequestBody List<RelationImportRow> rows) {
        return Result.success(graphService.importRelations(rows));
    }

    @RequirePermission(module = "GRAPH", permission = "update")
    @PostMapping("/analysis/recalculate")
    public Result<Map<String, Object>> recalculateAnalysis() {
        return Result.success(graphService.recalculateAllAnalysis());
    }

    // ========== 企业角色 ==========
    @RequirePermission(module = "GRAPH", permission = "view")
    @GetMapping("/enterprises/{id}/role")
    public Result<EnterpriseRole> getEnterpriseRole(@PathVariable Long id) {
        return Result.success(graphService.getEnterpriseRole(id));
    }

    @RequirePermission(module = "GRAPH", permission = "view")
    @GetMapping("/roles")
    public Result<List<EnterpriseRole>> getAllEnterpriseRoles() {
        return Result.success(graphService.getAllEnterpriseRoles());
    }

    @RequirePermission(module = "GRAPH", permission = "view")
    @GetMapping("/enterprises/{id}/position")
    public Result<EnterprisePositionAnalysis> getPosition(@PathVariable Long id) {
        return Result.success(graphService.getPositionAnalysis(id));
    }

    @RequirePermission(module = "GRAPH", permission = "view")
    @GetMapping("/positions")
    public Result<List<EnterprisePositionAnalysis>> getAllPositionAnalyses() {
        return Result.success(graphService.getAllPositionAnalyses());
    }

    // ========== 异常关系 ==========
    @RequirePermission(module = "GRAPH", permission = "view")
    @GetMapping("/abnormals")
    public Result<List<AbnormalRelation>> getAbnormals(@RequestParam(required = false) Long enterpriseId) {
        if (enterpriseId != null) {
            return Result.success(graphService.getAbnormalsByEnterprise(enterpriseId));
        }
        return Result.success(graphService.getAllAbnormals());
    }

    @RequirePermission(module = "GRAPH", permission = "view")
    @GetMapping("/enterprises/{id}/abnormals")
    public Result<List<AbnormalRelation>> getAbnormalsByEnterprise(@PathVariable Long id) {
        return Result.success(graphService.getAbnormalsByEnterprise(id));
    }

    @RequirePermission(module = "GRAPH", permission = "update")
    @PutMapping("/abnormals/{id}/status")
    public Result<Void> updateAbnormalStatus(@PathVariable Long id, @RequestBody java.util.Map<String, String> body) {
        graphService.updateAbnormalStatus(id, body.get("status"));
        return Result.success();
    }

    @RequirePermission(module = "GRAPH", permission = "update")
    @PostMapping("/abnormals/{id}/resolve")
    public Result<Void> resolveAbnormal(@PathVariable Long id) {
        graphService.resolveAbnormal(id);
        return Result.success();
    }
}
