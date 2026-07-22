package com.scfs.module.risk.mapper;

import com.scfs.module.risk.entity.RiskProfile;
import com.scfs.module.risk.entity.TransactionStability;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 风险画像 Mapper - 对应 RFC 表25~26（schema_risk）
 */
@Mapper
public interface RiskMapper {

    // 风险画像
    RiskProfile selectRiskProfileByApplication(@Param("applicationId") Long applicationId);

    List<RiskProfile> selectRiskProfilesByEnterprise(@Param("enterpriseId") Long enterpriseId);

    int insertRiskProfile(RiskProfile profile);

    // 交易稳定性
    TransactionStability selectTransactionStability(@Param("enterpriseId") Long enterpriseId);

    int insertTransactionStability(TransactionStability stability);

    int updateTransactionStability(TransactionStability stability);
}
