package com.scfs.module.graph.controller;

import com.scfs.common.core.PageQuery;
import com.scfs.common.core.PageResult;
import com.scfs.common.core.Result;
import com.scfs.common.security.RequirePermission;
import com.scfs.module.graph.entity.AbnormalRelation;
import com.scfs.module.graph.entity.Enterprise;
import com.scfs.module.graph.entity.EnterprisePositionAnalysis;
import com.scfs.module.graph.entity.EnterpriseRole;
import com.scfs.module.graph.service.GraphService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 图谱 Controller - 对应 RFC 3.x /api/graph
 */
@RestController
@RequestMapping("/api/graph")
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

    // ========== 企业角色 ==========
    @RequirePermission(module = "GRAPH", permission = "view")
    @GetMapping("/enterprises/{id}/role")
    public Result<EnterpriseRole> getEnterpriseRole(@PathVariable Long id) {
        return Result.success(graphService.getEnterpriseRole(id));
    }

    @RequirePermission(module = "GRAPH", permission = "view")
    @GetMapping("/enterprises/{id}/position")
    public Result<EnterprisePositionAnalysis> getPosition(@PathVariable Long id) {
        return Result.success(graphService.getPositionAnalysis(id));
    }

    // ========== 异常关系 ==========
    @RequirePermission(module = "GRAPH", permission = "view")
    @GetMapping("/enterprises/{id}/abnormals")
    public Result<List<AbnormalRelation>> getAbnormals(@PathVariable Long id) {
        return Result.success(graphService.getAbnormalsByEnterprise(id));
    }

    @RequirePermission(module = "GRAPH", permission = "update")
    @PutMapping("/abnormals/{id}/status")
    public Result<Void> updateAbnormalStatus(@PathVariable Long id, @RequestBody java.util.Map<String, String> body) {
        graphService.updateAbnormalStatus(id, body.get("status"));
        return Result.success();
    }
}
