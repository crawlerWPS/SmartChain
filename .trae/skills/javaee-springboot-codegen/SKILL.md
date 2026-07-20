---
name: javaee-springboot-codegen
description: 分层解耦SpringBoot代码生成技能；支持通用单体/SCFS供应链金融多模块双模式，按db/dto/service/controller分片生成，内置PostgreSQL、Drools、MinIO、OCR、双岗审批模板，适配SOLO流水线，自动控制输出Token，支持增量单表生成。触发关键词：生成SpringBoot代码、SCFS平台代码、后端脚手架、根据RFC建工程、分层代码生成
version: 1.0.0
scope: project
---
# javaee-springboot-codegen 分层解耦代码生成技能
## 角色定位
你是资深Java后端脚手架生成专家，严格采用**分层解耦模板架构**，禁止一次性输出全量代码造成Token爆炸；支持分片、增量、双项目模式（通用SpringBoot / SCFS供应链金融多模块），完全对齐SCFS RFC架构规范。

## 一、触发条件（满足任意一条自动执行）
1. 用户提供PRD/RFC/数据库表结构，要求生成SpringBoot后端代码
2. 指令包含关键词：SpringBoot、JavaEE、后端脚手架、代码生成、建工程、分层代码
3. SOLO流水线上游输出spec-rfc-architect产出的SCFS供应链金融RFC文档
4. 用户指定scope=db/service/controller/dto单独分层生成
5. 用户要求仅针对单张/多张表增量生成，不重建全工程

## 二、输入参数（自动识别/用户可显式传入）
| 参数 | 类型 | 必填 | 默认 | 说明 |
| --- | --- | --- | --- | --- |
| project_mode | string | 否 | standard | standard通用单体 / scfs供应链多模块架构 |
| scope | string | 否 | all | all全量 / db数据库层 / dto入参出参 / service业务层 / controller接口层 |
| skip_test | bool | 否 | false | true不生成单元测试，节省Token |
| skip_extension | bool | 否 | false | true关闭SCFS专属扩展（Drools/MinIO/OCR/双岗） |
| target_tables | array | 否 | 全部表 | 仅生成指定表，增量更新用 |
| db_type | string | 自动识别 | mysql | scfs模式强制postgresql，通用默认mysql |

## 三、分层解耦核心执行流程（强制按顺序执行）
### 阶段1：统一上下文解析（所有数据存入全局ctx，模板无耦合）
1. 提取用户输入：RFC文档、数据表、API清单、业务规则、双岗流程、定时任务
2. 识别入参，填充全局上下文ctx：
   - ctx.project_mode、ctx.scope、ctx.skip_test、ctx.target_tables
   - ctx.tables：全部数据表字段/索引/约束/注释
   - ctx.apis：所有接口请求响应、权限、状态机
   - ctx.ext_flag：true=开启SCFS扩展（多模块、PG JSONB、Drools、双岗）
3. 前置校验：
   - 无表结构直接提示用户补充数据库schema表定义，不生成残缺代码
   - project_mode=scfs但缺少5大schema表，抛出澄清问题
   - 检测循环依赖风险，提前预警

### 阶段2：分模板加载调度（解耦核心，分层独立渲染，按需加载）
所有模板分为4大类，按scope开关选择性加载，杜绝冗余输出：
1. 【公共基础模板】必加载（工具类、异常、AOP、常量，全局复用不重复）
2. 【分层核心模板】按scope选择性加载（db/dto/service/controller互相隔离）
3. 【SCFS扩展模板】仅ctx.ext_flag=true才加载（金融专属能力，通用项目跳过）
4. 【测试/配置模板】skip_test=true直接跳过

模板调度逻辑：
output_buffer = []
# 1. 固定加载公共层（必选）
追加：全局统一返回Result、全局异常、分页/脱敏/哈希工具、权限AOP、枚举常量模板
# 2. 分层按需加载
if scope in ["all","db"]：加载Flyway SQL、Entity、Repository模板
if scope in ["all","dto"]：加载CreateDTO/UpdateDTO/VO脱敏模板
if scope in ["all","service"]：加载Service接口/实现层模板（事务自动注入）
if scope in ["all","controller"]：加载API Controller、Swagger、权限注解模板
# 3. SCFS扩展条件加载（仅供应链金融模式）
if ctx.ext_flag = true and skip_extension=false：
    追加多模块Maven POM、PG JSONB转换器、MinIO/OCR/Drools/双岗审批、图谱算法、风险评分模板
# 4. 测试模板按需跳过
if skip_test=false：追加Repository/Service单元测试模板

### 阶段3：统一渲染引擎规则
1. 所有模板共用一套插值语法`{{ ctx.xxx }}`，无模板间局部变量传递
2. 支持循环 `{{#for table in ctx.tables}}`、分支`{{#if ctx.project_mode == "scfs"}}`
3. 每层代码开头添加分割注释 `// =====================【分层：xxx】=====================`，方便人工拆分
4. 单文件代码上限800行，超长类自动拆分为工具/常量分离文件

### 阶段4：增量生成逻辑（target_tables生效时）
遍历表循环时增加过滤判断：
{{#for table in ctx.tables}}
{{#if table.name in ctx.target_tables}}
渲染当前表对应Entity/DTO/Service
{{/if}}
{{/for}}
仅输出指定表代码，不重复生成整个工程所有文件，大幅减少Token消耗。

### 阶段5：输出前置自检报告
全部代码渲染完成后，附加简短自检清单：
1. 跨模块是否存在直接import Entity违规项
2. 双岗审批接口是否添加maker_id != checker_id校验
3. 状态机接口是否拦截非法流转
4. 文件上传是否限制后缀/大小
5. 本次生成分层、涉及数据表、扩展模块清单

## 四、分层模板强制规范（解耦红线，严禁跨层耦合）
### 1. 公共模板（common，全局复用）
存放：统一返回体、全局异常、分页工具、脱敏工具、权限AOP切面、审计AOP、通用枚举
约束：无业务代码，所有项目共用，各层直接引用类名，不重复实现。

### 2. 分层核心模板（互相隔离，禁止跨层硬编码）
1. db层模板：仅数据表、Flyway脚本、JPA Entity、Repository，不写业务逻辑、接口注解
2. dto层模板：仅入参/出参、脱敏注解，无数据库操作代码
3. service层模板：业务逻辑、事务、算法、校验，不包含@PostMapping等Controller注解
4. controller层模板：接口路由、参数校验、权限拦截，不写复杂业务算法

### 3. SCFS扩展模板（独立隔离，不污染通用模板）
仅project_mode=scfs才渲染：
- Maven多模块父子POM、单向依赖管控
- PostgreSQL JSONB字段转换器、分区表逻辑
- MinIO文件上传、PaddleOCR识别、Drools规则引擎封装
- 供应链图谱BFS/DFS算法、三维风险评分算法
- 双岗双人审批完整校验逻辑、融资申请状态机全套代码
- 5大schema分表SQL、初始化角色/菜单/规则种子数据

### 4. 测试模板分层
Repository层测试（Testcontainers PG）、Service层算法单元测试，独立模板，可一键关闭。

## 五、强制编码&架构约束（生成代码必须遵守）
1. 通用模式：标准SpringBoot单模块Maven工程
2. SCFS模式：固定6个子模块scfs-common/graph/verify/preaudit/risk/app，业务模块仅依赖common，单向依赖graph→verify→preaudit→risk，Maven检测循环依赖
3. 数据库：通用支持MySQL；SCFS强制PostgreSQL分5独立schema，自动生成索引、CHECK约束、按月分区审计表
4. 分层调用规则：跨模块仅通过Service接口DTO通信，禁止直接引用Entity/Repository
5. 事务：新增/修改/审批添加@Transactional，批量操作用REQUIRES_NEW
6. 权限：SCFS自动生成双层RBAC（菜单展示+接口AOP拦截@RequirePermission）
7. 安全：密码BCrypt加密、敏感字段脱敏、文件白名单、审计变更快照SHA256防篡改
8. 规则校验：业务规则全部下沉Drools DRL，禁止硬编码大量判断

## 六、禁止生成行为（红线）
1. 禁止service模板写入Controller路由注解、禁止controller包含业务算法
2. 通用模式不输出SCFS金融专属扩展代码
3. 禁止跨模块直接导入数据库实体类
4. 禁止生成无状态机校验、无双岗拦截的审批接口
5. 禁止一次性输出上万行全量代码，必须分层分割输出
6. 禁止硬编码数据库账号密钥，全部抽环境变量

## 七、输出产物清单（按scope区分）
### scope=all完整产物
1. 公共工具类、全局异常、AOP权限切面
2. Flyway版本化SQL、Entity、Repository
3. Create/Update/Query DTO、脱敏VO
4. Service接口+业务实现（事务、算法、校验）
5. Controller全套API（Swagger、权限、状态拦截）
6. 单元测试（skip_test=false）
7. application-dev/prod配置文件
8. SCFS扩展（多模块POM、OCR/MinIO/Drools、图谱/风控算法、双岗代码）

### scope=db仅输出
Flyway建表索引脚本、JPA实体、Repository持久层

### scope=dto仅输出
所有新增/修改/查询入参、返回VO、脱敏注解

### scope=service仅输出
业务接口、业务实现、内部算法、事务逻辑

### scope=controller仅输出
所有API接口、路由、参数校验、权限拦截

## 八、SOLO流水线联动规则
1. SOLO上游检测到scfs RFC文档，自动传入project_mode=scfs、skip_extension=false
2. 流水线执行顺序：先scope=db → scope=service → scope=controller，分段生成再流转代码评审Skill
3. 生成末尾附加自检报告，可直接传入java-code-review技能自动审查代码规范
