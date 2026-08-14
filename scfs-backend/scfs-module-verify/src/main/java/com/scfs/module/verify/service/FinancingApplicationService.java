package com.scfs.module.verify.service;

import com.scfs.common.audit.Audit;
import com.scfs.common.core.PageResult;
import com.scfs.common.enums.ApplicationStatus;
import com.scfs.common.enums.BusinessType;
import com.scfs.common.security.SecurityContextHelper;
import com.scfs.common.service.SysUserService;
import com.scfs.common.entity.SysUser;
import com.scfs.module.verify.entity.*;
import com.scfs.module.verify.mapper.VerifyMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

/**
 * 融资申请服务 - 对应 RFC 4.2.2 ApplicationService
 *
 * <p>关键策略：</p>
 * <ul>
 *   <li>状态机：DRAFT→SUBMITTED→PRE_AUDITING→VERIFYING→PENDING_DECISION→APPROVED/REJECTED</li>
 *   <li>乐观锁：update 时校验 version</li>
 *   <li>所有状态变更必须通过 transit() 方法，自动写入 application_status_history</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FinancingApplicationService {

    private final VerifyMapper verifyMapper;
    private final SecurityContextHelper securityContextHelper;
    private final SysUserService sysUserService;

    public FinancingApplication getById(Long id) {
        FinancingApplication application = verifyMapper.selectApplicationById(id);
        if (application != null && !visibleStatusesForCurrentRole().contains(application.getStatus())) {
            throw new IllegalArgumentException("申请不存在或当前角色无权查看该状态数据");
        }
        return application;
    }

    public FinancingApplication getByAppNo(String appNo) {
        return verifyMapper.selectApplicationByNo(appNo);
    }

    public PageResult<FinancingApplication> search(String status, Long submittedBy, Long enterpriseId, String keyword,
                                                    long offset, int size) {
        List<String> visibleStatuses = visibleStatusesForCurrentRole();
        if (status != null && !status.isBlank() && !visibleStatuses.contains(status)) {
            return PageResult.empty();
        }
        long total = verifyMapper.countApplications(status, visibleStatuses, submittedBy, enterpriseId, keyword);
        if (total == 0) {
            return PageResult.empty();
        }
        return PageResult.of(verifyMapper.selectApplicationPage(status, visibleStatuses, submittedBy, enterpriseId, keyword, offset, size),
                total);
    }

    private List<String> visibleStatusesForCurrentRole() {
        String role = securityContextHelper.getCurrentRoleCodeOrThrow();
        if ("RCO".equals(role)) {
            return List.of(ApplicationStatus.SUBMITTED.name(), ApplicationStatus.APPROVED.name(), ApplicationStatus.REJECTED.name());
        }
        if ("OPS".equals(role)) {
            return List.of(ApplicationStatus.PENDING_DECISION.name(), ApplicationStatus.APPROVED.name(), ApplicationStatus.REJECTED.name());
        }
        if ("RM".equals(role)) {
            return Arrays.stream(ApplicationStatus.values()).map(Enum::name).toList();
        }
        return Arrays.stream(ApplicationStatus.values())
                .filter(value -> value != ApplicationStatus.VERIFYING)
                .map(Enum::name)
                .toList();
    }

    /**
     * 创建草稿申请
     */
    @Audit(module = "VERIFY", action = "CREATE", targetType = "FINANCING_APPLICATION", targetIdExpr = "#result")
    @Transactional
    public Long createApplication(FinancingApplication application) {
        Long currentUserId = securityContextHelper.getCurrentUserIdOrThrow();
        validateTradeParties(application);
        application.setAppNo(generateAppNo());
        application.setSubmittedBy(currentUserId);
        application.setStatus(ApplicationStatus.DRAFT.name());
        application.setVersion(0);
        verifyMapper.insertApplication(application);
        log.info("[Application] 草稿已创建: appNo={}, id={}", application.getAppNo(), application.getId());

        // 写入状态历史
        recordStatusHistory(application.getId(), null, ApplicationStatus.DRAFT.name(), currentUserId, "草稿创建");
        return application.getId();
    }

    @Audit(module = "VERIFY", action = "UPDATE", targetType = "FINANCING_APPLICATION", targetIdExpr = "#application.id")
    @Transactional
    public void updateApplication(FinancingApplication application) {
        FinancingApplication existing = verifyMapper.selectApplicationById(application.getId());
        if (existing == null) {
            throw new IllegalArgumentException("申请不存在");
        }
        // 仅 DRAFT 状态可修改
        if (!ApplicationStatus.DRAFT.name().equals(existing.getStatus())) {
            throw new IllegalStateException("仅草稿状态可修改");
        }
        validateTradeParties(application);
        application.setVersion(existing.getVersion());
        verifyMapper.updateApplication(application);
    }

    public List<ApplicationCustomer> searchCustomers(String keyword) {
        return verifyMapper.selectApplicationCustomers(keyword, false);
    }

    public List<ApplicationCustomer> searchBuyerCustomers(String keyword) {
        return verifyMapper.selectApplicationCustomers(keyword, true);
    }

    public List<ApplicationCustomer> searchSellerCustomers(Long buyerEnterpriseId, String keyword) {
        if (buyerEnterpriseId == null) {
            return List.of();
        }
        return verifyMapper.selectSellerCustomersByBuyer(buyerEnterpriseId, keyword);
    }

    @Transactional
    public Long saveCustomer(ApplicationCustomer customer) {
        if (customer.getName() == null || customer.getName().isBlank()
                || customer.getUscc() == null || customer.getUscc().isBlank()) {
            throw new IllegalArgumentException("客户名称和统一社会信用代码不能为空");
        }
        if (customer.getEnterpriseId() == null) verifyMapper.insertApplicationCustomer(customer);
        else if (verifyMapper.updateApplicationCustomer(customer) == 0) throw new IllegalArgumentException("客户不存在");
        return customer.getEnterpriseId();
    }

    @Transactional
    public void maintainTradeRelation(Long buyerId, Long sellerId) {
        if (buyerId == null || sellerId == null || buyerId.equals(sellerId)) throw new IllegalArgumentException("请选择不同的买方和卖方客户");
        if (verifyMapper.countEnterpriseById(buyerId) == 0 || verifyMapper.countEnterpriseById(sellerId) == 0) throw new IllegalArgumentException("买方或卖方客户不存在");
        if (verifyMapper.countRelationByEnterpriseIds(buyerId, sellerId) == 0) verifyMapper.insertTradeRelation(buyerId, sellerId);
    }

    @Transactional
    public void assign(Long id, Long handlerId) {
        if (handlerId == null) throw new IllegalArgumentException("审核人不能为空");
        FinancingApplication app = verifyMapper.selectApplicationById(id);
        if (app == null) throw new IllegalArgumentException("申请不存在");
        ApplicationStatus status = ApplicationStatus.valueOf(app.getStatus());
        if (status == ApplicationStatus.DRAFT || status.isFinalState()) throw new IllegalStateException("当前状态不允许指派审核人");
        int version = app.getVersion() + 1;
        verifyMapper.updateApplicationStatus(id, app.getStatus(), handlerId, version);
        recordStatusHistory(id, app.getStatus(), app.getStatus(), securityContextHelper.getCurrentUserIdOrThrow(), "指派审核人: " + handlerId);
    }

    private void validateTradeParties(FinancingApplication application) {
        Long buyer = application.getBuyerEnterpriseId();
        Long seller = application.getSellerEnterpriseId();
        if (buyer == null || seller == null) {
            throw new IllegalArgumentException("请选择买方和卖方");
        }
        if (buyer.equals(seller)) {
            throw new IllegalArgumentException("买方和卖方不能相同");
        }
        if (verifyMapper.countEnterpriseById(buyer) == 0) {
            throw new IllegalArgumentException("买方客户不存在");
        }
        if (verifyMapper.countEnterpriseById(seller) == 0) {
            throw new IllegalArgumentException("卖方客户不存在");
        }
        if (verifyMapper.countRelationByEnterpriseIds(buyer, seller) == 0) {
            throw new IllegalArgumentException("买方和卖方不存在供应链关系");
        }
        // 融资客户即卖方，兼容既有 enterprise_id 业务逻辑。
        application.setEnterpriseId(seller);
    }

    /**
     * 提交申请（DRAFT → SUBMITTED）
     */
    @Audit(module = "VERIFY", action = "SUBMIT", targetType = "FINANCING_APPLICATION", targetIdExpr = "#id")
    @Transactional
    public void submit(Long id) {
        Long currentUserId = securityContextHelper.getCurrentUserIdOrThrow();
        FinancingApplication app = verifyMapper.selectApplicationById(id);
        if (app == null) {
            throw new IllegalArgumentException("申请不存在");
        }
        ApplicationStatus current = ApplicationStatus.valueOf(app.getStatus());
        if (!current.canTransitTo(ApplicationStatus.SUBMITTED)) {
            throw new IllegalStateException("当前状态不允许提交: " + app.getStatus());
        }
        transit(app, ApplicationStatus.SUBMITTED, currentUserId, "客户经理提交");
        app.setSubmittedAt(Instant.now());
        verifyMapper.updateApplication(app);
    }

    /**
     * 推进到预审（SUBMITTED → PRE_AUDITING）
     */
    @Transactional
    public void moveToPreAudit(Long id) {
        Long currentUserId = securityContextHelper.getCurrentUserIdOrThrow();
        transitTo(id, ApplicationStatus.PRE_AUDITING, currentUserId, "进入预审");
    }

    /**
     * 推进到核验（PRE_AUDIT_PASSED → VERIFYING）
     */
    @Transactional
    public void moveToVerify(Long id) {
        Long currentUserId = securityContextHelper.getCurrentUserIdOrThrow();
        transitTo(id, ApplicationStatus.VERIFYING, currentUserId, "进入核验");
    }

    /**
     * 预审通过
     */
    @Transactional
    public void preAuditPassed(Long id) {
        Long currentUserId = securityContextHelper.getCurrentUserIdOrThrow();
        transitTo(id, ApplicationStatus.PRE_AUDIT_PASSED, currentUserId, "预审通过");
    }

    /**
     * 预审失败（返回 SUBMITTED 待补正）
     */
    @Transactional
    public void preAuditFailed(Long id, String reason) {
        Long currentUserId = securityContextHelper.getCurrentUserIdOrThrow();
        FinancingApplication app = verifyMapper.selectApplicationById(id);
        if (app == null) {
            throw new IllegalArgumentException("申请不存在");
        }
        ApplicationStatus current = ApplicationStatus.valueOf(app.getStatus());
        if (!current.canTransitTo(ApplicationStatus.PRE_AUDIT_FAILED)) {
            throw new IllegalStateException("当前状态不允许预审失败: " + app.getStatus());
        }
        transit(app, ApplicationStatus.PRE_AUDIT_FAILED, currentUserId, reason);
        // 写入状态历史后再次回到 SUBMITTED（待补正）
        transit(app, ApplicationStatus.SUBMITTED, currentUserId, "退回待补正");
    }

    /**
     * 进入风险评分
     */
    @Transactional
    public void moveToRiskScoring(Long id) {
        Long currentUserId = securityContextHelper.getCurrentUserIdOrThrow();
        transitTo(id, ApplicationStatus.RISK_SCORING, currentUserId, "进入风险评分");
    }

    /**
     * 进入待决策
     */
    @Transactional
    public void moveToPendingDecision(Long id) {
        Long currentUserId = securityContextHelper.getCurrentUserIdOrThrow();
        transitTo(id, ApplicationStatus.PENDING_DECISION, currentUserId, "进入待决策");
    }

    /**
     * 审批通过
     */
    @Audit(module = "VERIFY", action = "APPROVE", targetType = "FINANCING_APPLICATION", targetIdExpr = "#id")
    @Transactional
    public void approve(Long id, String remark) {
        Long currentUserId = securityContextHelper.getCurrentUserIdOrThrow();
        requireReviewRemark(remark);
        FinancingApplication app = verifyMapper.selectApplicationById(id);
        if (app == null) {
            throw new IllegalArgumentException("申请不存在");
        }
        requireDecisionRoleAndStatus(app);
        ApplicationStatus current = ApplicationStatus.valueOf(app.getStatus());
        if (!current.canTransitTo(ApplicationStatus.APPROVED)) {
            throw new IllegalStateException("当前状态不允许审批通过: " + app.getStatus());
        }
        transit(app, ApplicationStatus.APPROVED, currentUserId, remark);
        app.setApprovedAt(Instant.now());
        verifyMapper.updateApplication(app);
    }

    /**
     * 风控人员无法判断时升级给运营主管处理。
     */
    @Audit(module = "VERIFY", action = "ESCALATE", targetType = "FINANCING_APPLICATION", targetIdExpr = "#id")
    @Transactional
    public void escalateToOps(Long id, Long supervisorId, String remark) {
        Long currentUserId = securityContextHelper.getCurrentUserIdOrThrow();
        if (!"RCO".equals(securityContextHelper.getCurrentRoleCodeOrThrow())) {
            throw new IllegalStateException("仅风控审核员可以升级运营主管");
        }
        requireReviewRemark(remark);
        if (supervisorId == null || supervisorId <= 0) {
            throw new IllegalArgumentException("请输入有效的运营主管用户ID");
        }
        FinancingApplication app = verifyMapper.selectApplicationById(id);
        if (app == null) {
            throw new IllegalArgumentException("申请不存在");
        }
        requireDecisionRoleAndStatus(app);
        ApplicationStatus current = ApplicationStatus.valueOf(app.getStatus());
        if (current != ApplicationStatus.PENDING_DECISION
                && !current.canTransitTo(ApplicationStatus.PENDING_DECISION)) {
            throw new IllegalStateException("当前状态不允许升级运营主管: " + app.getStatus());
        }
        SysUser supervisor = sysUserService.getById(supervisorId);
        if (supervisor == null) {
            throw new IllegalArgumentException("运营主管用户不存在");
        }
        if (supervisor.getStatus() == null || supervisor.getStatus() != 1) {
            throw new IllegalStateException("运营主管账号未启用");
        }
        if (!"OPS".equals(supervisor.getRoleCode())) {
            throw new IllegalArgumentException("指定用户不是运营主管");
        }
        String fromStatus = app.getStatus();
        verifyMapper.updateApplicationStatus(id, ApplicationStatus.PENDING_DECISION.name(),
                supervisorId, app.getVersion() + 1);
        recordStatusHistory(id, fromStatus, ApplicationStatus.PENDING_DECISION.name(), currentUserId,
                "升级运营主管（" + (supervisor.getRealName() == null ? supervisorId : supervisor.getRealName())
                        + "）：" + remark.trim());
        log.info("[Application] 已升级运营主管: appNo={}, supervisorId={}, operatorId={}",
                app.getAppNo(), supervisorId, currentUserId);
    }

    /**
     * 审批拒绝
     */
    @Audit(module = "VERIFY", action = "REJECT", targetType = "FINANCING_APPLICATION", targetIdExpr = "#id")
    @Transactional
    public void reject(Long id, String remark) {
        Long currentUserId = securityContextHelper.getCurrentUserIdOrThrow();
        requireReviewRemark(remark);
        FinancingApplication app = verifyMapper.selectApplicationById(id);
        if (app == null) {
            throw new IllegalArgumentException("申请不存在");
        }
        requireDecisionRoleAndStatus(app);
        ApplicationStatus current = ApplicationStatus.valueOf(app.getStatus());
        if (!current.canTransitTo(ApplicationStatus.REJECTED)) {
            throw new IllegalStateException("当前状态不允许拒绝: " + app.getStatus());
        }
        transit(app, ApplicationStatus.REJECTED, currentUserId, remark);
    }

    /**
     * 撤销审批
     */
    @Audit(module = "VERIFY", action = "REVOKE", targetType = "FINANCING_APPLICATION", targetIdExpr = "#id")
    @Transactional
    public void revokeApproval(Long id, String reason) {
        Long currentUserId = securityContextHelper.getCurrentUserIdOrThrow();
        FinancingApplication app = verifyMapper.selectApplicationById(id);
        if (app == null) {
            throw new IllegalArgumentException("申请不存在");
        }
        ApplicationStatus current = ApplicationStatus.valueOf(app.getStatus());
        ApplicationStatus target = current == ApplicationStatus.APPROVED
                ? ApplicationStatus.APPROVED_REVOKED
                : ApplicationStatus.REJECTED_REVOKED;
        if (!current.canTransitTo(target)) {
            throw new IllegalStateException("当前状态不允许撤销: " + app.getStatus());
        }
        transit(app, target, currentUserId, reason);
    }

    private void transitTo(Long id, ApplicationStatus target, Long operatorId, String remark) {
        FinancingApplication app = verifyMapper.selectApplicationById(id);
        if (app == null) {
            throw new IllegalArgumentException("申请不存在");
        }
        ApplicationStatus current = ApplicationStatus.valueOf(app.getStatus());
        if (!current.canTransitTo(target)) {
            throw new IllegalStateException(String.format(
                    "状态不允许从 %s 流转到 %s", current, target));
        }
        transit(app, target, operatorId, remark);
    }

    private void transit(FinancingApplication app, ApplicationStatus target, Long operatorId, String remark) {
        String fromStatus = app.getStatus();
        app.setStatus(target.name());
        app.setCurrentHandler(null);
        verifyMapper.updateApplicationStatus(app.getId(), app.getStatus(), null, app.getVersion() + 1);
        recordStatusHistory(app.getId(), fromStatus, target.name(), operatorId, remark);
        log.info("[Application] 状态变更: appNo={}, {} -> {}, operatorId={}",
                app.getAppNo(), fromStatus, target, operatorId);
    }

    private void recordStatusHistory(Long applicationId, String from, String to, Long operatorId, String remark) {
        ApplicationStatusHistory history = new ApplicationStatusHistory();
        history.setApplicationId(applicationId);
        history.setFromStatus(from);
        history.setToStatus(to);
        history.setOperatorId(operatorId);
        history.setRemark(remark);
        verifyMapper.insertStatusHistory(history);
    }

    private void requireReviewRemark(String remark) {
        if (remark == null || remark.trim().isEmpty()) {
            throw new IllegalArgumentException("请填写审核意见");
        }
    }

    private void requireDecisionRoleAndStatus(FinancingApplication application) {
        String role = securityContextHelper.getCurrentRoleCodeOrThrow();
        boolean allowed = ("RCO".equals(role) && ApplicationStatus.SUBMITTED.name().equals(application.getStatus()))
                || ("OPS".equals(role) && ApplicationStatus.PENDING_DECISION.name().equals(application.getStatus()));
        if (!allowed) {
            throw new IllegalStateException("当前角色不能操作该状态的融资申请");
        }
    }

    private String generateAppNo() {
        return "APP-" + Instant.now().toEpochMilli();
    }

    public List<ApplicationStatusHistory> getStatusHistory(Long applicationId) {
        return verifyMapper.selectStatusHistory(applicationId);
    }
}
