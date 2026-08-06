package com.scfs.module.graph.service;

import com.scfs.common.core.PageResult;
import com.scfs.module.graph.entity.AbnormalRelation;
import com.scfs.module.graph.entity.Enterprise;
import com.scfs.module.graph.entity.EnterprisePositionAnalysis;
import com.scfs.module.graph.entity.EnterpriseRole;
import com.scfs.module.graph.entity.SupplyChainRelation;
import com.scfs.module.graph.mapper.GraphMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 图谱服务 - 对应 RFC 4.2.1 GraphService
 *
 * <p>关键能力：</p>
 * <ul>
 *   <li>企业/关系查询</li>
 *   <li>1~N 跳关系扩展（DFS）</li>
 *   <li>企业角色识别（核心/关键供应商/一级/二级/普通/边缘）</li>
 *   <li>位置分析（核心链路/上下游稳定性）</li>
 *   <li>异常关系检测（快速扩张/环形路径/关联方交易）</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GraphService {

    private final GraphMapper graphMapper;

    // ========== 企业 ==========

    public Enterprise getEnterpriseById(Long id) {
        return graphMapper.selectEnterpriseById(id);
    }

    public Enterprise getEnterpriseByUscc(String uscc) {
        return graphMapper.selectEnterpriseByUscc(uscc);
    }

    public PageResult<Enterprise> searchEnterprises(String keyword, long offset, int size) {
        long total = graphMapper.countEnterprises(keyword);
        if (total == 0) {
            return PageResult.empty();
        }
        return PageResult.of(graphMapper.searchEnterprises(keyword, offset, size), total);
    }

    // ========== 关系图谱 ==========

    /**
     * 查询企业 N 跳关系（DFS 扩展）
     * @param enterpriseId 起点企业 ID
     * @param level 扩展层级（1=直接，2=二级）
     * @return 节点 + 边
     */
    public Map<String, Object> getRelationGraph(Long enterpriseId, int level) {
        Map<String, Object> graph = new HashMap<>();

        // 收集节点 ID 集合
        Map<Long, Enterprise> nodesMap = new HashMap<>();
        List<SupplyChainRelation> allEdges = new ArrayList<>();

        // 起点
        Enterprise start = graphMapper.selectEnterpriseById(enterpriseId);
        if (start == null) {
            graph.put("nodes", List.of());
            graph.put("edges", List.of());
            return graph;
        }
        nodesMap.put(start.getId(), start);

        // 1 跳
        List<SupplyChainRelation> level1 = graphMapper.selectRelationsByEnterprise(enterpriseId, 1);
        for (SupplyChainRelation rel : level1) {
            Long otherId = rel.getFromEnterpriseId().equals(enterpriseId) ? rel.getToEnterpriseId() : rel.getFromEnterpriseId();
            if (!nodesMap.containsKey(otherId)) {
                Enterprise other = graphMapper.selectEnterpriseById(otherId);
                if (other != null) {
                    nodesMap.put(otherId, other);
                }
            }
        }
        allEdges.addAll(level1);

        // 2 跳（可选）
        if (level >= 2) {
            List<SupplyChainRelation> level2 = graphMapper.selectRelationsByEnterprise(enterpriseId, 2);
            for (SupplyChainRelation rel : level2) {
                Long otherId = rel.getFromEnterpriseId().equals(enterpriseId) ? rel.getToEnterpriseId() : rel.getFromEnterpriseId();
                if (!nodesMap.containsKey(otherId)) {
                    Enterprise other = graphMapper.selectEnterpriseById(otherId);
                    if (other != null) {
                        nodesMap.put(otherId, other);
                    }
                }
            }
            allEdges.addAll(level2);
        }

        // 组装为前端 G6 期望的格式：nodes[{id,label,isCore,data}], edges[{source,target,label,relationType,data}]
        List<Map<String, Object>> nodes = new ArrayList<>();
        for (Enterprise ent : nodesMap.values()) {
            Map<String, Object> node = new HashMap<>();
            node.put("id", String.valueOf(ent.getId()));
            node.put("label", ent.getName());
            node.put("enterpriseId", ent.getId());
            node.put("isCore", ent.getId().equals(enterpriseId));
            node.put("data", ent);
            nodes.add(node);
        }

        List<Map<String, Object>> edges = new ArrayList<>();
        for (SupplyChainRelation rel : allEdges) {
            Map<String, Object> edge = new HashMap<>();
            edge.put("source", String.valueOf(rel.getFromEnterpriseId()));
            edge.put("target", String.valueOf(rel.getToEnterpriseId()));
            edge.put("label", rel.getRelationType());
            edge.put("relationType", rel.getRelationType());
            edge.put("weight", rel.getTotalTransactions());
            edge.put("data", rel);
            edges.add(edge);
        }

        graph.put("nodes", nodes);
        graph.put("edges", edges);
        return graph;
    }

    public List<SupplyChainRelation> getAllRelations() {
        return graphMapper.selectAllRelations();
    }

    /**
     * 获取全部企业与关系图谱（不指定起点企业）
     * @return 节点 + 边
     */
    public Map<String, Object> getAllRelationGraph() {
        Map<String, Object> graph = new HashMap<>();

        // 全部企业作为节点
        List<Enterprise> enterprises = graphMapper.searchEnterprises(null, 0, 1000);
        Map<Long, Enterprise> nodesMap = new HashMap<>();
        for (Enterprise ent : enterprises) {
            nodesMap.put(ent.getId(), ent);
        }

        // 全部关系作为边
        List<SupplyChainRelation> allEdges = graphMapper.selectAllRelations();

        // 查询所有企业角色，找出核心企业 ID 集合
        Set<Long> coreIds = new HashSet<>();
        for (Enterprise ent : enterprises) {
            EnterpriseRole role = graphMapper.selectRoleByEnterprise(ent.getId());
            if (role != null && "CORE".equals(role.getRole())) {
                coreIds.add(ent.getId());
            }
        }

        // 组装为前端 G6 期望的格式
        List<Map<String, Object>> nodes = new ArrayList<>();
        for (Enterprise ent : nodesMap.values()) {
            Map<String, Object> node = new HashMap<>();
            node.put("id", String.valueOf(ent.getId()));
            node.put("label", ent.getName());
            node.put("enterpriseId", ent.getId());
            node.put("isCore", coreIds.contains(ent.getId()));
            node.put("data", ent);
            nodes.add(node);
        }

        List<Map<String, Object>> edges = new ArrayList<>();
        for (SupplyChainRelation rel : allEdges) {
            Map<String, Object> edge = new HashMap<>();
            edge.put("source", String.valueOf(rel.getFromEnterpriseId()));
            edge.put("target", String.valueOf(rel.getToEnterpriseId()));
            edge.put("label", rel.getRelationType());
            edge.put("relationType", rel.getRelationType());
            edge.put("weight", rel.getTotalTransactions());
            edge.put("data", rel);
            edges.add(edge);
        }

        graph.put("nodes", nodes);
        graph.put("edges", edges);
        return graph;
    }

    // ========== 企业角色识别 ==========

    public EnterpriseRole getEnterpriseRole(Long enterpriseId) {
        return graphMapper.selectRoleByEnterprise(enterpriseId);
    }

    public List<EnterpriseRole> getAllEnterpriseRoles() {
        return graphMapper.selectAllRoles();
    }

    /**
     * 计算并存储企业角色（异步调用）
     * @param enterpriseId 目标企业
     * @param coreEnterpriseId 核心企业 ID
     */
    @Transactional
    public void calculateEnterpriseRole(Long enterpriseId, Long coreEnterpriseId) {
        EnterpriseRole existing = graphMapper.selectRoleByEnterprise(enterpriseId);

        EnterpriseRole role = existing != null ? existing : new EnterpriseRole();
        role.setEnterpriseId(enterpriseId);
        role.setCoreEnterpriseId(coreEnterpriseId);

        // 获取直接关系
        List<SupplyChainRelation> relations = graphMapper.selectRelationsByEnterprise(enterpriseId, 1);

        // 计算合作年限（简化）
        LocalDate now = LocalDate.now();
        LocalDate firstCoop = relations.stream()
                .map(SupplyChainRelation::getFirstCoopDate)
                .filter(java.util.Objects::nonNull)
                .min(LocalDate::compareTo)
                .orElse(now);
        BigDecimal years = BigDecimal.valueOf(java.time.Period.between(firstCoop, now).getYears());
        role.setCoopDurationYears(years);

        // 合作企业数量
        long coopCount = relations.stream()
                .map(r -> r.getFromEnterpriseId().equals(enterpriseId) ? r.getToEnterpriseId() : r.getFromEnterpriseId())
                .distinct()
                .count();
        role.setCoopEnterpriseCount((int) coopCount);

        // 角色判定
        String roleCode = determineRole(relations, enterpriseId, coreEnterpriseId);
        role.setRole(roleCode);

        // 影响力 / 信用评级（简化逻辑）
        role.setInfluenceLevel(coopCount >= 10 ? "HIGH" : coopCount >= 3 ? "MID" : "LOW");
        role.setCredibilityLevel(years.compareTo(BigDecimal.valueOf(3)) >= 0 ? "HIGH" : "MID");

        role.setCalculatedAt(Instant.now());

        if (existing == null) {
            graphMapper.insertEnterpriseRole(role);
        } else {
            graphMapper.updateEnterpriseRole(role);
        }
        log.info("[Graph] 企业角色已更新: enterpriseId={}, role={}", enterpriseId, roleCode);
    }

    private String determineRole(List<SupplyChainRelation> relations, Long enterpriseId, Long coreEnterpriseId) {
        if (enterpriseId.equals(coreEnterpriseId)) {
            return "CORE";
        }
        // 与核心企业直接关联
        boolean directToCore = relations.stream()
                .anyMatch(r -> coreEnterpriseId.equals(r.getFromEnterpriseId()) || coreEnterpriseId.equals(r.getToEnterpriseId()));
        if (directToCore) {
            long coopCount = relations.size();
            if (coopCount >= 8) {
                return "KEY_SUPPLIER";
            }
            return "TIER1";
        }
        // 间接关联
        return "TIER2";
    }

    // ========== 位置分析 ==========

    public EnterprisePositionAnalysis getPositionAnalysis(Long enterpriseId) {
        return graphMapper.selectPositionAnalysis(enterpriseId);
    }

    public List<EnterprisePositionAnalysis> getAllPositionAnalyses() {
        return graphMapper.selectAllPositionAnalyses();
    }

    @Transactional
    public void analyzeEnterprisePosition(Long enterpriseId, Long coreEnterpriseId) {
        EnterprisePositionAnalysis existing = graphMapper.selectPositionAnalysis(enterpriseId);
        EnterprisePositionAnalysis analysis = existing != null ? existing : new EnterprisePositionAnalysis();
        analysis.setEnterpriseId(enterpriseId);

        List<SupplyChainRelation> relations = graphMapper.selectRelationsByEnterprise(enterpriseId, 2);
        boolean inCoreChain = relations.stream()
                .anyMatch(r -> coreEnterpriseId.equals(r.getFromEnterpriseId()) || coreEnterpriseId.equals(r.getToEnterpriseId()));
        analysis.setInCoreChain(inCoreChain);

        // 距核心企业层级（简化）
        analysis.setDistanceToCore(inCoreChain ? 1 : 2);

        // 上下游稳定性
        boolean hasUpstream = relations.stream().anyMatch(r -> r.getRelationType().equals("SUPPLY") && r.getToEnterpriseId().equals(enterpriseId));
        boolean hasDownstream = relations.stream().anyMatch(r -> r.getRelationType().equals("PURCHASE") && r.getFromEnterpriseId().equals(enterpriseId));
        long upstreamCount = relations.stream().filter(r -> r.getRelationType().equals("SUPPLY")).count();
        long downstreamCount = relations.stream().filter(r -> r.getRelationType().equals("PURCHASE")).count();
        analysis.setUpstreamStable(hasUpstream && upstreamCount >= 2);
        analysis.setDownstreamStable(hasDownstream && downstreamCount >= 2);

        // 信用评级
        String credibility;
        String reason;
        if (inCoreChain && analysis.getUpstreamStable() && analysis.getDownstreamStable()) {
            credibility = "HIGH";
            reason = "位于核心链路，上下游稳定";
        } else if (inCoreChain) {
            credibility = "MID";
            reason = "位于核心链路，但上下游不稳定";
        } else if (analysis.getUpstreamStable() || analysis.getDownstreamStable()) {
            credibility = "MID";
            reason = "未在核心链路，但单侧稳定";
        } else {
            credibility = "LOW";
            reason = "未在核心链路，上下游不稳定";
        }
        analysis.setCredibility(credibility);
        analysis.setCredibilityReason(reason);
        analysis.setCalculatedAt(Instant.now());

        if (existing == null) {
            graphMapper.insertPositionAnalysis(analysis);
        } else {
            graphMapper.updatePositionAnalysis(analysis);
        }
        log.info("[Graph] 位置分析完成: enterpriseId={}, credibility={}", enterpriseId, credibility);
    }

    // ========== 异常关系预警 ==========

    public List<AbnormalRelation> getAbnormalsByEnterprise(Long enterpriseId) {
        return graphMapper.selectAbnormalsByEnterprise(enterpriseId);
    }

    public List<AbnormalRelation> getAllAbnormals() {
        return graphMapper.selectAllAbnormals();
    }

    @Transactional
    public void updateAbnormalStatus(Long id, String status) {
        graphMapper.updateAbnormalStatus(id, status);
    }

    @Transactional
    public void resolveAbnormal(Long id) {
        graphMapper.updateAbnormalStatus(id, "RESOLVED");
    }
}
