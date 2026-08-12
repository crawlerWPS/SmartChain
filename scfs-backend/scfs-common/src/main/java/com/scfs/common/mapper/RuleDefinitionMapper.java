package com.scfs.common.mapper;

import com.scfs.common.entity.RuleChangeLog;
import com.scfs.common.entity.RuleDefinition;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 规则定义 Mapper - 对应 RFC 表6 rule_definition / 表7 rule_change_log
 */
@Mapper
public interface RuleDefinitionMapper {

    RuleDefinition selectById(@Param("id") Long id);

    RuleDefinition selectByCode(@Param("ruleCode") String ruleCode);

    List<RuleDefinition> selectPage(@Param("category") String category,
                                    @Param("status") Short status,
                                    @Param("keyword") String keyword,
                                    @Param("offset") long offset,
                                    @Param("size") int size);

    long countAll(@Param("category") String category, @Param("status") Short status, @Param("keyword") String keyword);

    List<RuleDefinition> selectByCategory(@Param("category") String category);

    int insert(RuleDefinition rule);

    int update(RuleDefinition rule);

    int updateStatus(@Param("id") Long id, @Param("status") Short status, @Param("version") Integer version);

    // 变更日志
    int insertChangeLog(RuleChangeLog changeLog);

    RuleChangeLog selectChangeLogById(@Param("id") Long id);

    RuleChangeLog selectPendingChangeLogByRuleId(@Param("ruleId") Long ruleId);

    List<RuleChangeLog> selectChangeLogsByRuleId(@Param("ruleId") Long ruleId);

    List<RuleChangeLog> selectPendingChangeLogs(@Param("offset") long offset, @Param("size") int size);

    long countPendingChangeLogs();

    int updateChangeLogStatus(@Param("id") Long id,
                               @Param("status") String status,
                               @Param("checkerId") Long checkerId,
                               @Param("rejectReason") String rejectReason);
}
