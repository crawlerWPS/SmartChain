package com.scfs.common.mapper;

import com.scfs.common.entity.MaterialChecklistTemplate;
import com.scfs.common.entity.RiskWeightConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 风险权重配置 Mapper - 对应 RFC 表8 risk_weight_config
 */
@Mapper
public interface RiskWeightConfigMapper {

    RiskWeightConfig selectById(@Param("id") Long id);

    RiskWeightConfig selectEnabled();

    List<RiskWeightConfig> selectPage(@Param("status") String status,
                                      @Param("offset") long offset,
                                      @Param("size") int size);

    long countAll(@Param("status") String status);

    int insert(RiskWeightConfig config);

    int update(RiskWeightConfig config);

    int updateStatus(@Param("id") Long id, @Param("status") String status);

    // 材料清单模板
    int insertTemplate(MaterialChecklistTemplate template);

    MaterialChecklistTemplate selectTemplateByBusinessType(@Param("businessType") String businessType);

    List<MaterialChecklistTemplate> selectAllTemplates();

    int updateTemplate(MaterialChecklistTemplate template);
}
