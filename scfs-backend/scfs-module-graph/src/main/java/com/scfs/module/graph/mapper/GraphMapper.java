package com.scfs.module.graph.mapper;

import com.scfs.module.graph.entity.Enterprise;
import com.scfs.module.graph.entity.AbnormalRelation;
import com.scfs.module.graph.entity.EnterprisePositionAnalysis;
import com.scfs.module.graph.entity.EnterpriseRole;
import com.scfs.module.graph.entity.SupplyChainRelation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 图谱模块 Mapper - 对应 RFC 表9~13（schema_graph）
 */
@Mapper
public interface GraphMapper {

    // 企业
    Enterprise selectEnterpriseById(@Param("id") Long id);

    Enterprise selectEnterpriseByUscc(@Param("uscc") String uscc);

    List<Enterprise> searchEnterprises(@Param("keyword") String keyword,
                                        @Param("offset") long offset,
                                        @Param("size") int size);

    long countEnterprises(@Param("keyword") String keyword);

    int insertEnterprise(Enterprise enterprise);

    int updateEnterprise(Enterprise enterprise);

    // 关系
    List<SupplyChainRelation> selectRelationsByEnterprise(@Param("enterpriseId") Long enterpriseId,
                                                            @Param("level") Integer level);

    List<SupplyChainRelation> selectAllRelations();

    int insertRelation(SupplyChainRelation relation);

    int batchInsertRelations(@Param("list") List<SupplyChainRelation> list);

    // 角色
    EnterpriseRole selectRoleByEnterprise(@Param("enterpriseId") Long enterpriseId);

    List<EnterpriseRole> selectRolesByCoreEnterprise(@Param("coreEnterpriseId") Long coreEnterpriseId);

    List<EnterpriseRole> selectAllRoles();

    int insertEnterpriseRole(EnterpriseRole role);

    int updateEnterpriseRole(EnterpriseRole role);

    // 位置分析
    EnterprisePositionAnalysis selectPositionAnalysis(@Param("enterpriseId") Long enterpriseId);

    List<EnterprisePositionAnalysis> selectAllPositionAnalyses();

    int insertPositionAnalysis(EnterprisePositionAnalysis analysis);

    int updatePositionAnalysis(EnterprisePositionAnalysis analysis);

    // 异常关系
    List<AbnormalRelation> selectAbnormalsByEnterprise(@Param("enterpriseId") Long enterpriseId);

    List<AbnormalRelation> selectAllAbnormals();

    int insertAbnormalRelation(AbnormalRelation abnormal);

    int batchInsertAbnormals(@Param("list") List<AbnormalRelation> list);

    int updateAbnormalStatus(@Param("id") Long id, @Param("status") String status);
}
