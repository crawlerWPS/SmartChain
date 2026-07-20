---
name: scfs-frontend-codegen
description: SCFS供应链金融风控平台前端专用代码生成技能；读取scfs_platform_rfc.md完整RFC文档，自动产出Umi4+React18+Ant Design Pro5+AntV G6全套前端工程；自动生成路由、RBAC权限、65个接口请求、全业务页面、图谱画布、双岗审批组件、材料上传/报表导出；支持scope分片输出控制Token消耗，可和javaee-springboot-codegen后端技能、SOLO调度流水线联动，完全对齐RFC架构、菜单、数据表、API、业务流程
version: 1.0.0
scope: project
trigger_keywords: SCFS前端、供应链金融前端、umi pro代码生成、图谱页面、融资申请页面、规则双岗、风险画像、材料上传、前端脚手架
---
# scfs-frontend-codegen 前端代码生成技能
## 一、核心基准与联动规则
### 1. 唯一基准文档
强制以附件`scfs_platform_rfc.md`作为全部生成数据源，所有页面、接口、菜单、数据表、状态机、权限、业务流程必须100匹配RFC定义，不允许自由发挥、自定义游离页面。
### 2. 固定技术栈（严格遵循RFC 1.4前端选型）
- 框架：UmiJS 4 + TypeScript + React18
- UI组件库：Ant Design Pro 5
- 图可视化：AntV G6 5（供应链关系图谱专用）
- HTTP请求：统一封装axios拦截器（适配JWT Bearer鉴权）
- 权限体系：双层RBAC（菜单可见+按钮API权限，匹配后端sys_role_menu / sys_role_permission）
- 文件处理：xlsx表格导出、PDF预览、MinIO文件上传封装
### 3. SO流水线强制联动顺序
完整流水线固定串行执行，本技能**必须等待javaee-springboot-codegen后端技能生成完成后再调用**：
1. spec-rfc技能产出SCFS完整RFC
2. javaee-springboot-codegen生成后端多模块、65个API、数据库结构
3. 自动调度scfs-frontend-codegen生成前端工程
4. 生成完成后自动输出自检清单，校验是否对齐RFC全部规范
### 4. 分片输出机制（核心省Token设计）
支持scope入参控制只生成指定分层，禁止一次性输出数万行全量代码，分片可选值：
`scope=route / api / component / page / graph / all`
额外控制开关：
- skip_export=true：不生成导出组件，减少输出
- skip_graph=true：跳过G6图谱页面（非图谱场景使用）
- target_module=["融资申请","规则配置"]：仅生成指定业务模块页面

## 二、输入参数说明（自动识别/手动传入）
| 参数 | 类型 | 默认值 | 作用 |
|------|------|--------|------|
| project_scfs | boolean | true | 固定SCFS专用模式，不可关闭 |
| scope | string | all | 分片生成范围，控制输出体量 |
| skip_export | boolean | false | 关闭导出相关组件/页面 |
| skip_graph | boolean | false | 关闭G6图谱画布代码 |
| target_module | string[] | 全部模块 | 按需只生成指定菜单页面 |

## 三、前置自动解析逻辑（执行第一步）
技能启动时自动读取附件`scfs_platform_rfc.md`，提取5大类核心数据作为生成依据：
1. 菜单树：RFC 2.3b完整树形目录、菜单/按钮编码、路由path、组件路径、图标、排序、权限标识
2. API集合：RFC第3章全部65个接口（请求方式、路径、入参/出参JSON、分页、错误码、模块操作权限）
3. 数据模型：5个schema全部表、字段、枚举（风险等级/申请状态/材料类型/角色编码）、脱敏字段（USCC、手机号、法人）
4. 业务约束：融资申请状态机、规则双岗审批流程、OCR材料识别逻辑、图谱层级限制、风控评分规则
5. 前端目录规范：严格对齐RFC 1.5 scfs-frontend目录结构

## 四、分层解耦生成目录（强制输出结构，不可改动）
scfs-frontend/
├── .umirc.ts                # Umi 全局配置、路由、代理
├── dockerfile               # 前端容器构建
├── nginx.conf               # 反向代理（对接后端 /api/v1）
├── package.json             # 锁定全套依赖版本
├── tsconfig.json            # 严格 TS 类型校验
├── src/
├── access/              # RBAC 权限鉴权、v-permission 指令
├── api/                 # 65 个接口按模块拆分请求文件
├── app.ts               # 全局拦截器、JWT 处理、全局提示
├── components/          # 全局复用业务组件
│   ├── common/          # 分页、搜索、状态标签、弹窗
│   ├── graph/           # G6 图谱画布封装
│   ├── upload/          # 多文件上传 + OCR 预览
│   ├── approval/        # 双岗审批操作栏
│   └── export/          # XLSX/PDF 导出组件
├── layouts/             # ProLayout 侧边菜单布局
├── pages/               # 业务页面（严格匹配 RFC 菜单树）
│   ├── workspace/       # 工作台 - 待办 / 运营监控
│   ├── graph/           # 供应链图谱 4 个页面
│   ├── audit/           # 审核中心（申请 / 材料 / 核验 / 画像）
│   ├── rule/            # 规则、权重、模板双岗页面
│   └── audit-trail/     # 审计日志查询
├── routes/              # 动态路由配置文件
├── services/            # axios 统一请求封装
├── store/               # 全局用户 / 菜单 / 权限状态
├── types/               # 全量 TS 类型（表、API、枚举）
└── utils/               # 脱敏、日期、哈希、状态转换工具

## 五、各分层生成规则（按scope分片执行）
### 分层1：scope=route 路由&权限层（优先必生成）
1. access目录：
   - 读取RFC角色编码（RM/RCO/OPS_MAKER/OPS_CHECKER/ADMIN/AUDIT）
   - 生成`access.ts`鉴权函数，匹配后端module+操作权限
   - 封装全局`v-permission`自定义指令，按钮自动绑定rule:create、rule:approve等权限标识
2. routes.ts：
   1:1复刻RFC菜单树，区分目录/菜单/按钮，自动绑定icon、sort、路由路径；路由守卫拦截未登录、无权限跳转403页面
3. app.ts：
   请求拦截器自动携带`Bearer JWT`；响应拦截统一处理RFC标准错误码（1001参数错误/1003无权限/2001OCR异常），全局loading、消息弹窗统一封装

### 分层2：scope=api 接口请求层（依赖后端API产出）
按业务模块拆分ts文件，覆盖RFC全部65个接口，每个接口自动生成：
- 完整TS入参/返回类型（复用types层定义）
- 请求method、完整基础路径`/api/v1`
- 分页参数统一封装PageQuery
- 文件上传multipart、文件流下载单独封装
- 注释标注对应RFC接口编号IF-XXX
模块拆分：auth.ts、user.ts、role.ts、application.ts、material.ts、graph.ts、verify.ts、preaudit.ts、risk.ts、rule.ts、audit.ts

### 分层3：scope=types 全局TS类型（全数据表+枚举）
1. 全部PostgreSQL表对应TS实体，数字Decimal适配前端
2. 业务枚举自动生成：
   融资申请状态、风险等级LOW/MID/HIGH、材料类型、角色编码、核验结果、审批状态
3. 通用封装：统一后端Result分页结构、脱敏字段类型（手机号/信用代码）

### 分层4：scope=component 通用业务组件（可单独生成）
所有页面复用组件，内置RFC业务逻辑：
1. FileUpload：限制50MB、文件白名单、OCR置信度展示、手动修正入口
2. GraphCanvas：G6封装，多层级图谱、节点区分核心企业、右键展开上下游、导出图片
3. ApprovalBar：自动区分经办/复核角色，隐藏无权限操作按钮
4. StatusTag：状态自动配色（审批/风险等级）
5. ExportBtn：一键导出PDF/XLSX
6. SupplementDrawer：补正清单弹窗
7. VerifyReportPreview：核验报告PDF预览

### 分层5：scope=page 业务页面（核心产出）
页面统一标准结构：顶部搜索区 + 表格列表 + 操作按钮 + 新增/审批弹窗/详情抽屉；严格匹配RFC菜单划分，全部页面实现：
1. 工作台：我的待办（按角色过滤）、运营风险统计图表
2. 图谱模块：企业关系图谱、企业角色、位置分析、异常预警列表
3. 审核中心：融资申请CRUD、材料上传识别、核验报告、预审补正、风险画像雷达图
4. 规则配置：规则双岗经办/复核、风险权重配置、材料模板管理
5. 审计查询：全量日志筛选、批量导出、变更详情弹窗
6. 系统管理：用户/角色/菜单树形配置

页面强制业务规则：
1. 状态机控制：仅在合法状态展示操作按钮（如DRAFT仅显示提交，终态隐藏审批）
2. 权限控制：所有创建/审批/导出按钮自动挂载`v-permission`，无权限自动隐藏
3. 数据合规：USCC、手机号、法人自动脱敏展示
4. 异常兜底：捕获2001 OCR服务异常弹窗提示人工识别

### 分层6：scope=graph 图谱专项页面（AntV G6）
1. 节点/边自定义渲染：核心企业高亮、区分交易类型线条
2. 层级筛选（限制最大2层，防止性能爆炸）
3. 右键菜单：查看企业画像、展开上下游
4. 缩放、框选、图片导出功能
5. 对接图谱IF-023接口，自动渲染后端返回nodes/edges数据

## 六、强制业务约束（生成不可违背红线）
1. 双岗审批逻辑：同一账号不能同时展示经办+复核按钮，匹配数据库`maker_id != checker_id`约束
2. 状态机完整对齐RFC融资申请、规则变更流转，非法操作弹窗提示错误码1005
3. 文件上传严格校验：仅允许pdf/jpg/png/docx/xlsx，单文件≤50MB，拦截可执行文件
4. 权限双层隔离：菜单控制侧边展示、API按钮控制后端接口访问，缺一不可
5. 所有接口统一走src/api封装方法，禁止页面硬编码请求地址
6. 所有数据表字段必须生成TS类型，禁止页面any任意类型
7. 核验报告、审计日志导出携带SHA256哈希完整性标识
8. OCR置信度＜60自动弹窗提示人工指定材料类型

## 七、自动输出配套工程配置文件
1. package.json：锁定Umi4、antd pro5、@antv/g6、xlsx、pdf依赖版本
2. nginx.conf：前端静态托管、/api转发至后端8080端口、文件上传50MB限制
3. Dockerfile：多阶段构建前端镜像，适配RFC docker-compose部署
4. .umirc.ts：路由、代理、全局权限配置
5. tsconfig：开启严格类型校验，禁止隐式any

## 八、禁止生成行为（SC业务红线，绝对不允许）
1. 脱离scfs_platform_rfc文档自定义页面、接口、菜单
2. 省略v-permission权限指令，按钮无权限控制
3. 简化图谱G6能力（不支持多层、无异常节点标记）
4. 跳过双岗角色区分逻辑，同一账号可同时经办复核
5. 文件上传无大小、后缀拦截逻辑
6. 硬编码后端接口地址，不统一封装api层
7. 缺失TS类型，页面大量any
8. 缺失脱敏处理，明文展示信用代码、手机号

## 九、SOLO Agent联动配置规则
在SOLO主控提示词追加联动调度逻辑：
1. 后端javaee-springboot-codegen完整生成、自检通过后，自动调用scfs-frontend-codegen；
2. 前端生成scope默认all，生成完成输出自检报告：
   - 菜单数量是否与RFC完全匹配
   - 65个API接口是否全部覆盖
   - 图谱、双岗、文件核心组件是否齐全
   - 权限、状态机逻辑是否合规
3. 前端产出完成后，可流转java-code-review技能做前端代码规范校验

## 十、输出后自检清单（每次生成附带）
生成代码末尾自动输出简短自检清单，用于人工核对：
1. 菜单树：与RFC 2.3b完全一致，无缺失/多余页面
2. API：覆盖RFC全部65个接口，路径/请求方式匹配
3. 权限：所有操作按钮挂载v-permission指令
4. 双岗：经办、复核按钮做角色隔离
5. 图谱：G画布支持多层、异常节点标记
6. 文件：上传大小/白名单校验完整
7. 状态机：所有操作受状态限制，非法流转拦截
8. TS：无any逃逸类型，全实体/枚举定义齐全
