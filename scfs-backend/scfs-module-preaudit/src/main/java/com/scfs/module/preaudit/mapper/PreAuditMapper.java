package com.scfs.module.preaudit.mapper;

import com.scfs.module.preaudit.entity.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 预审模块 Mapper - 对应 RFC 表21~24（schema_preaudit）
 */
@Mapper
public interface PreAuditMapper {

    // 完整性检查
    MaterialCompletenessResult selectCompleteness(@Param("applicationId") Long applicationId);

    int insertCompleteness(MaterialCompletenessResult result);

    int updateCompleteness(MaterialCompletenessResult result);

    // 有效性检查
    MaterialValidityResult selectValidity(@Param("applicationId") Long applicationId);

    int insertValidity(MaterialValidityResult result);

    int updateValidity(MaterialValidityResult result);

    // 企业信息一致性 - 主表
    EnterpriseInfoConsistencyResult selectConsistency(@Param("applicationId") Long applicationId);

    int insertConsistency(EnterpriseInfoConsistencyResult result);

    // 企业信息一致性 - 明细
    List<EnterpriseInfoMismatchDetail> selectMismatchDetails(@Param("resultId") Long resultId);

    int batchInsertMismatchDetails(@Param("list") List<EnterpriseInfoMismatchDetail> list);

    // 补正清单
    SupplementList selectSupplementList(@Param("applicationId") Long applicationId);

    int insertSupplementList(SupplementList supplementList);

    int updateSupplementListStatus(@Param("id") Long id, @Param("status") String status);
}
