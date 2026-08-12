package com.scfs.module.verify.mapper;

import com.scfs.module.verify.entity.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * 核验模块 Mapper - 对应 RFC 表14~19（schema_verify）
 */
@Mapper
public interface VerifyMapper {

    // 融资申请
    FinancingApplication selectApplicationById(@Param("id") Long id);

    FinancingApplication selectApplicationByNo(@Param("appNo") String appNo);

    List<FinancingApplication> selectApplicationPage(@Param("status") String status,
                                                       @Param("submittedBy") Long submittedBy,
                                                     @Param("enterpriseId") Long enterpriseId,
                                                     @Param("keyword") String keyword,
                                                       @Param("offset") long offset,
                                                       @Param("size") int size);

    long countApplications(@Param("status") String status,
                           @Param("submittedBy") Long submittedBy,
                           @Param("enterpriseId") Long enterpriseId,
                           @Param("keyword") String keyword);

    int insertApplication(FinancingApplication application);

    int updateApplication(FinancingApplication application);

    List<ApplicationCustomer> selectApplicationCustomers(@Param("keyword") String keyword);

    long countEnterpriseById(@Param("enterpriseId") Long enterpriseId);

    long countRelationByEnterpriseIds(@Param("buyerEnterpriseId") Long buyerEnterpriseId,
                                      @Param("sellerEnterpriseId") Long sellerEnterpriseId);

    int updateApplicationStatus(@Param("id") Long id,
                                 @Param("status") String status,
                                 @Param("currentHandler") Long currentHandler,
                                 @Param("version") Integer version);

    int updateApplicationHandler(@Param("id") Long id,
                                 @Param("currentHandler") Long currentHandler,
                                 @Param("version") Integer version);

    // 申请状态历史
    int insertStatusHistory(ApplicationStatusHistory history);

    List<ApplicationStatusHistory> selectStatusHistory(@Param("applicationId") Long applicationId);

    // 申请材料
    List<ApplicationMaterial> selectMaterialsByApplication(@Param("applicationId") Long applicationId);

    ApplicationMaterial selectMaterialById(@Param("id") Long id);

    int insertMaterial(ApplicationMaterial material);

    int updateMaterialType(@Param("id") Long id,
                           @Param("materialType") String materialType,
                           @Param("identifiedBy") String identifiedBy);

    // 材料识别结果
    MaterialRecognitionResult selectRecognitionResult(@Param("applicationMaterialId") Long applicationMaterialId);

    int deleteRecognitionResult(@Param("applicationMaterialId") Long applicationMaterialId);

    int insertRecognitionResult(MaterialRecognitionResult result);

    int updateRecognitionResult(MaterialRecognitionResult result);

    // 核验项检查结果
    List<VerifyCheckResult> selectCheckResultsByApplication(@Param("applicationId") Long applicationId);

    int insertCheckResult(VerifyCheckResult result);

    // 核验报告
    VerifyReport selectReportByApplication(@Param("applicationId") Long applicationId);

    VerifyReport selectReportById(@Param("id") Long id);

    VerifyReport selectReportByNo(@Param("reportNo") String reportNo);

    int insertReport(VerifyReport report);

    // 重复融资查询
    long countApprovedApplicationsByEnterprise(@Param("enterpriseId") Long enterpriseId,
                                                @Param("businessType") String businessType);
}
