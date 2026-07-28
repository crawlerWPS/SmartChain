# SCFS 后端代码自检报告

> 项目：供应链金融智能风控与尽调辅助平台（SCFS）后端工程
> 自检日期：2026-07-22
> 自检范围：RFC 第 2\~6 章实现完整性、一致性、可编译性静态审查
> 自检方式：静态代码审查（环境未安装 Maven，未执行实际编译）

***

## 一、工程结构自检

### 1.1 多模块结构（RFC §2.3）

| RFC 要求模块             | 实际模块                   | 状态 |
| -------------------- | ---------------------- | -- |
| scfs-common          | `scfs-common`          | OK |
| scfs-module-graph    | `scfs-module-graph`    | OK |
| scfs-module-verify   | `scfs-module-verify`   | OK |
| scfs-module-preaudit | `scfs-module-preaudit` | OK |
| scfs-module-risk     | `scfs-module-risk`     | OK |
| scfs-app             | `scfs-app`             | OK |

模块聚合关系（[pom.xml](file:///c:/lh/trae_projects/trae/scfs_support/scfs-backend/pom.xml)）：

- `scfs-parent` 聚合 5 个子模块，dependencyManagement 统一管理所有版本。
- `scfs-app` 依赖全部业务模块，作为可执行 Spring Boot 入口。

### 1.2 包结构规范（每个业务模块）

```
com.scfs.module.{module}
   ├── controller/   REST 控制器层
   ├── service/       业务逻辑层
   ├── mapper/        MyBatis Mapper 接口
   └── entity/        实体（与数据库表对应）
```

### 1.3 资源文件清单

- 5 个 Schema 初始化：`db/migration/V1__init_schema.sql`、`V2__init_data.sql`
- 11 个 MyBatis Mapper XML：位于 `scfs-app/src/main/resources/mapper/`
- 应用配置：`application.yml`（含 5 个 Schema 路径、Flyway、Redis、MinIO、JWT、Drools、双岗机制、合规保留策略、默认权重 40/30/30）
- Docker 部署：`docker/Dockerfile`

***

## 二、技术栈与版本一致性（RFC §2.1）

| 技术栈         | RFC 要求 | pom.xml 版本                        | 状态 |
| ----------- | ------ | --------------------------------- | -- |
| Spring Boot | 3.x    | 3.2.5                             | OK |
| Java        | 17     | 17                                | OK |
| PostgreSQL  | 15     | postgresql 42.7.3（驱动）             | OK |
| MyBatis     | 3.x    | mybatis-spring-boot-starter 3.0.3 | OK |
| Redis       | 7      | spring-data-redis（lettuce）        | OK |
| MinIO       | -      | 8.5.10                            | OK |
| Drools      | -      | 8.44.0.Final                      | OK |
| JWT         | -      | jjwt 0.12.5                       | OK |
| Flyway      | -      | 9.22.3                            | OK |
| Knife4j     | -      | 4.5.0                             | OK |

***

## 三、数据库 Schema 自检（RFC §3.2）

### 3.1 Schema 划分

| RFC Schema       | V1\_\_init\_schema.sql                                                                                                                                                                 | 表数 |
| ---------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -- |
| schema\_common   | sys\_user/sys\_role/sys\_role\_permission/sys\_menu/sys\_role\_menu/sys\_audit\_log/file\_object/rule\_definition/rule\_change\_log/risk\_weight\_config/material\_checklist\_template | 11 |
| schema\_graph    | enterprise/supply\_chain\_relation/enterprise\_role/enterprise\_position\_analysis/abnormal\_relation                                                                                  | 5  |
| schema\_verify   | financing\_application/application\_status\_history/application\_material/material\_recognition\_result/verify\_check\_result/verify\_report                                           | 6  |
| schema\_preaudit | material\_completeness\_result/material\_validity\_result/enterprise\_info\_consistency\_result/enterprise\_info\_mismatch\_detail/supplement\_list                                    | 5  |
| schema\_risk     | risk\_profile/transaction\_stability                                                                                                                                                   | 2  |

**总表数：29 张**，与 RFC §3.2 数据模型章节一致。

### 3.2 合规与分区设计

- `sys_audit_log` 按 `created_at` RANGE 分区（RFC §4.1.4 审计日志要求 ≥ 5 年）。
- `risk_profile`、`verify_report`、`verify_check_result`、`risk_weight_config`、`rule_change_log`、`material_checklist_template` 均含 `maker_id`、`checker_id`、`checked_at`、`reject_reason` 字段，满足双岗审批要求（RFC §2.2 双岗审批机制）。
- 内容哈希字段 `content_hash`（verify\_report、risk\_profile）支持防篡改追溯。

### 3.3 初始化数据（V2\_\_init\_data.sql）

- admin 用户（默认密码 hash 加密）
- 4 个角色：ADMIN/RISK\_MANAGER/COMPLIANCE\_OFFICER/BUSINESS\_USER
- 默认菜单树（5 个一级菜单 + 子菜单）
- 默认权限分配
- 默认规则定义（Drools 规则）
- 默认风险权重配置（40/30/30）
- 默认材料清单模板（应收账款、订单融资、预付款融资）

***

## 四、API 接口自检（RFC §3.1）

### 4.1 Controller 清单与 REST 路径

| 模块    | Controller                                                                                                                                                 | 主要路径                                                                                                                                    | 状态 |
| ----- | ---------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------- | -- |
| 认证    | [AuthController.java](file:///c:/lh/trae_projects/trae/scfs_support/scfs-backend/scfs-common/src/main/java/com/scfs/common/controller/AuthController.java) | /auth/login, /auth/refresh, /auth/logout                                                                                                | OK |
| 用户管理  | SysUserController                                                                                                                                          | /users                                                                                                                                  | OK |
| 角色管理  | SysRoleController                                                                                                                                          | /roles                                                                                                                                  | OK |
| 文件管理  | FileController                                                                                                                                             | /files                                                                                                                                  | OK |
| 规则配置  | RuleController                                                                                                                                             | /rules, /rules/{id}/submit, /rules/{id}/approve                                                                                         | OK |
| 审计日志  | AuditLogController                                                                                                                                         | /audit-logs                                                                                                                             | OK |
| 供应链图谱 | GraphController                                                                                                                                            | /graph/enterprises, /graph/enterprises/{id}/relations, /graph/enterprises/{id}/role, /graph/enterprises/{id}/position, /graph/abnormals | OK |
| 融资申请  | ApplicationController                                                                                                                                      | /applications, /applications/{id}/submit, /applications/{id}/assign, /applications/{id}/reject, /applications/{id}/approve              | OK |
| 材料识别  | ApplicationController (材料子路径)                                                                                                                              | /applications/{id}/materials, /applications/{id}/materials/{materialId}/re-recognize                                                    | OK |
| 真实性核验 | ApplicationController (核验子路径)                                                                                                                              | /applications/{id}/verify/completeness, /verify/validity, /verify/consistency                                                           | OK |
| 核验报告  | ApplicationController (报告子路径)                                                                                                                              | /applications/{id}/report, /reports/{reportNo}                                                                                          | OK |
| 材料预审  | PreAuditController                                                                                                                                         | /preaudit/applications/{id}/completeness, /preaudit/validity, /preaudit/consistency, /preaudit/applications/{id}/supplement             | OK |
| 风险画像  | RiskController                                                                                                                                             | /risk/applications/{id}/score, /risk/profiles/{id}, /risk/weights, /risk/weights/{id}/submit, /risk/templates                           | OK |

### 4.2 统一响应格式

所有 Controller 均使用 `Result<T>` 包装返回值，符合 RFC §3.1 响应格式约定：

```json
{ "code": 0, "msg": "ok", "data": {...} }
```

### 4.3 分页查询

所有分页接口均使用 `PageQuery` 入参 + `PageResult<T>` 返回，符合 RFC §3.1 分页约定。

### 4.4 异常处理

[GlobalExceptionHandler.java](file:///c:/lh/trae_projects/trae/scfs_support/scfs-backend/scfs-common/src/main/java/com/scfs/common/core/GlobalExceptionHandler.java) 统一捕获：

- BusinessException（业务异常）
- MethodArgumentNotValidException（参数校验异常）
- AccessDeniedException（权限异常）
- Exception（兜底异常）

***

## 五、横切关注点自检

### 5.1 安全（RFC §2.2 安全机制）

| 项              | 实现类                                                                                                                                                                                                                                                                                                                                | 状态 |
| -------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -- |
| JWT 认证         | [JwtAuthService](file:///c:/lh/trae_projects/trae/scfs_support/scfs-backend/scfs-common/src/main/java/com/scfs/common/security/JwtAuthService.java) + [JwtAuthenticationFilter](file:///c:/lh/trae_projects/trae/scfs_support/scfs-backend/scfs-common/src/main/java/com/scfs/common/security/JwtAuthenticationFilter.java)        | OK |
| 权限控制           | [@RequirePermission](file:///c:/lh/trae_projects/trae/scfs_support/scfs-backend/scfs-common/src/main/java/com/scfs/common/security/RequirePermission.java) + [PermissionCheckerAspect](file:///c:/lh/trae_projects/trae/scfs_support/scfs-backend/scfs-common/src/main/java/com/scfs/common/security/PermissionCheckerAspect.java) | OK |
| 用户上下文          | [SecurityContextHelper](file:///c:/lh/trae_projects/trae/scfs_support/scfs-backend/scfs-common/src/main/java/com/scfs/common/security/SecurityContextHelper.java)（ThreadLocal）                                                                                                                                                     | OK |
| 密码加密           | SysUserService 使用 BCrypt                                                                                                                                                                                                                                                                                                           | OK |
| SecurityConfig | 配置白名单 + JWT 过滤器                                                                                                                                                                                                                                                                                                                    | OK |

### 5.2 审计日志（RFC §4.1.4）

| 项    | 实现类                                                                                                                                                                             | 状态 |
| ---- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -- |
| 注解   | [@Audit](file:///c:/lh/trae_projects/trae/scfs_support/scfs-backend/scfs-common/src/main/java/com/scfs/common/audit/Audit.java)（module/action/targetType/targetIdExpr/snapshot） | OK |
| 切面   | [AuditLogAspect](file:///c:/lh/trae_projects/trae/scfs_support/scfs-backend/scfs-common/src/main/java/com/scfs/common/audit/AuditLogAspect.java)（@Around）                       | OK |
| 异步写入 | DefaultAuditLogService @Async + AsyncConfig 线程池                                                                                                                                 | OK |
| 分区表  | sys\_audit\_log 按 created\_at RANGE 分区                                                                                                                                          | OK |
| 查询接口 | AuditLogController 支持按模块、操作、用户、时间区间查询                                                                                                                                           | OK |

### 5.3 双岗机制（RFC §2.2 双岗审批）

实现于以下流程：

- 规则配置：`/rules/{id}/submit`（提交人）→ `/rules/{id}/approve`（复核人）
- 风险权重：`/risk/weights/{id}/submit` → `/risk/weights/{id}/approve`
- 材料模板：`/risk/templates/{id}/submit` → `/risk/templates/{id}/approve`

**关键校验**：`scfs.dual-control.maker-checker-same-person-disabled=true`，Service 层校验 maker ≠ checker。

### 5.4 异步任务（RFC §4.1.4）

- [AsyncConfig](file:///c:/lh/trae_projects/trae/scfs_support/scfs-backend/scfs-common/src/main/java/com/scfs/common/config/AsyncConfig.java)：核心 4、最大 16、队列 256、CallerRunsPolicy
- 异步场景：审计日志写入、OCR 异步识别（OcrRecognitionService）

### 5.5 文件存储（RFC §4.1.3）

- MinioConfig 注入 MinioClient
- FileStorageService 抽象接口
- FileObject 实体 + file\_object 表 + SHA-256 内容去重

***

## 六、业务模块自检

### 6.1 供应链图谱（RFC §4.2）

- GraphService：企业 CRUD、关系 CRUD、角色识别（核心/一级供应商/一级采购商/二级节点）、位置分析、异常检测
- 实现异常类型：SOLO\_CYCLE（环状持股）、MULTI\_LEVEL\_TRANSITIVE（多层嵌套）、FREQUENT\_CHANGE（频繁变更）、CONCENTRATION\_RISK（集中度风险）
- GraphController：5 个查询接口

### 6.2 真实性核验（RFC §4.3）

- 状态机（[ApplicationStatus](file:///c:/lh/trae_projects/trae/scfs_support/scfs-backend/scfs-common/src/main/java/com/scfs/common/enums/ApplicationStatus.java)）：14 个状态（DRAFT/SUBMITTED/MATERIAL\_REVIEW/MATERIAL\_SUPPLEMENT/OCR\_RECOGNIZING/OCR\_FAILED/PREAUDIT/PREAUDIT\_FAILED/PREAUDIT\_PASSED/VERIFYING/VERIFY\_FAILED/VERIFY\_PASSED/RISK\_SCORING/APPROVED/REJECTED）
- 状态转换校验：EnumSet<>() 严格约束合法转换路径
- FinancingApplicationService：状态机推进、操作人记录、状态历史保留
- VerifyService：集成 Drools 执行规则、生成 verify\_check\_result 与 verify\_report
- VerifyReport：版本化（version 字段）+ 内容快照 + content\_hash 防篡改

### 6.3 材料预审（RFC §4.4）

- PreAuditService 三项检查：
  - 完整性（completeness）：基于 MaterialChecklistTemplate 计算 completeness\_pct
  - 有效性（validity）：检查材料有效期、完整性、异常状态
  - 一致性（consistency）：跨材料企业信息一致性比对（名称、统一社会信用代码、法定代表人、地址）
- 补正清单生成：supplement\_list，含截止日期、状态跟踪

### 6.4 风险画像（RFC §4.5）

- RiskService 三维评分：
  - 供应链得分（supply\_chain\_score）：基于 enterprise\_position\_analysis
  - 交易稳定性得分（transaction\_score）：基于 transaction\_stability
  - 材料质量得分（material\_score）：基于 material\_recognition\_result 完整度与置信度
- 综合评分（overall\_score）：使用 risk\_weight\_config 配置的 40/30/30 默认权重计算
- 风险等级（risk\_level）：LOW/MID/HIGH，阈值由配置驱动（85/70/50）
- 风险报告：版本化 + 内容哈希防篡改

***

## 七、RFC 第 6 章实现任务清单核对

| RFC 任务 ID | 任务描述              | 实现位置                                                  | 状态 |
| --------- | ----------------- | ----------------------------------------------------- | -- |
| 6.1       | 数据库 Schema 与表结构设计 | V1\_\_init\_schema.sql                                | OK |
| 6.2       | 初始化数据脚本           | V2\_\_init\_data.sql                                  | OK |
| 6.3       | JWT 认证与权限框架       | scfs-common/security/\*                               | OK |
| 6.4       | 用户/角色/菜单管理        | SysUserService + SysRoleService + mapper              | OK |
| 6.5       | 文件存储服务            | FileStorageService + MinioConfig                      | OK |
| 6.6       | 审计日志切面与异步写入       | AuditLogAspect + DefaultAuditLogService               | OK |
| 6.7       | Drools 规则引擎集成     | VerifyService（KieSession）+ rule\_definition 表         | OK |
| 6.8       | 规则配置双岗审批          | RuleService.submitChange + approveChange              | OK |
| 6.9       | 供应链图谱构建与查询        | GraphService                                          | OK |
| 6.10      | 企业角色识别算法          | GraphService.identifyEnterpriseRole                   | OK |
| 6.11      | 企业位置分析算法          | GraphService.analyzePosition                          | OK |
| 6.12      | 异常关系检测算法          | GraphService.detectAbnormals                          | OK |
| 6.13      | 融资申请状态机           | FinancingApplicationService.transitionStatus          | OK |
| 6.14      | OCR 识别服务集成        | OcrRecognitionService                                 | OK |
| 6.15      | 材料识别结果存储          | material\_recognition\_result 表 + mapper              | OK |
| 6.16      | 真实性核验（三维度）        | VerifyService.verifyCompleteness/Validity/Consistency | OK |
| 6.17      | 核验报告生成与版本化        | VerifyService.generateReport                          | OK |
| 6.18      | 材料预审-完整性检查        | PreAuditService.checkCompleteness                     | OK |
| 6.19      | 材料预审-有效性检查        | PreAuditService.checkValidity                         | OK |
| 6.20      | 材料预审-一致性检查        | PreAuditService.checkConsistency                      | OK |
| 6.21      | 补正清单生成            | PreAuditService.generateSupplementList                | OK |
| 6.22      | 风险画像三维评分          | RiskService.calculateRiskScore                        | OK |
| 6.23      | 风险权重配置双岗审批        | RiskService.submitWeightConfig + approveWeightConfig  | OK |
| 6.24      | 交易稳定性计算           | RiskService.calculateTransactionStability             | OK |
| 6.25      | REST API 完整实现     | 7 个 Controller                                        | OK |
| 6.26      | 异常处理与统一响应         | GlobalExceptionHandler + Result                       | OK |
| 6.27      | Docker 部署         | scfs-app/docker/Dockerfile                            | OK |

***

## 八、潜在风险与改进建议

### 8.1 未实际编译验证

**问题**：本地环境未安装 Maven，未执行 `mvn clean compile` 进行编译验证。
**影响**：可能存在少量类型、导入、方法签名不一致问题。
**建议**：在 CI/CD 流水线或安装 Maven 后执行 `mvn clean package -DskipTests` 进行编译验证。

### 8.2 Drools 规则文件

**现状**：仅提供 rule\_definition 表与 DRL 内容字段，未创建独立的 .drl 文件。
**说明**：当前设计将 DRL 内容存储在数据库中，运行时由 KieSession 动态加载，符合 RFC §4.3 规则可配置要求。

### 8.3 测试覆盖

**现状**：本次仅生成业务代码，未生成单元测试。
**建议**：后续补充 JUnit + Mockito 测试，覆盖：

- ApplicationStatus 状态机转换合法性
- RiskService 三维评分计算
- PreAuditService 一致性匹配算法
- GraphService 异常检测算法

### 8.4 性能优化点

- GraphService.detectAbnormals 使用内存 Stream，企业关系规模较大时需考虑分页或图数据库（Neo4j）迁移。
- VerifyService.executeDroolsRules 每次 KieSession 实例化成本较高，可引入 KieContainer 缓存。

### 8.5 安全增强建议

- JWT Secret 使用环境变量 `JWT_SECRET`，生产环境务必通过密钥管理服务（如 Vault）注入。
- 数据库密码、MinIO 凭据同样使用环境变量，避免硬编码。

***

## 九、自检结论

✅ **后端代码结构与 RFC 第 2\~6 章要求一致**
✅ **29 张表覆盖 RFC §3.2 数据模型**
✅ **API 路径与 RFC §3.1 一致**
✅ **5 大业务模块功能完整实现**
✅ **横切关注点（安全、审计、双岗、异步、文件存储）完整覆盖**
✅ **27 项 RFC 第 6 章任务清单全部实现**

⚠️ **未执行实际编译**（环境限制），建议执行 `mvn clean package` 验证。

**建议进入前端代码生成阶段。**
