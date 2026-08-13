# SCFS 供应链金融智能风控与尽调辅助平台

供应链金融风控平台，覆盖供应链图谱、融资申请审核、材料 OCR 核验、预审补正、风险画像、规则配置、审计追溯等全流程，支持多角色 RBAC 权限与双岗审批机制。

## 一、技术栈

| 层 | 技术 | 版本 |
| --- | --- | --- |
| 后端框架 | Spring Boot | 3.2.5 |
| JDK | Java | 17 |
| ORM | MyBatis Spring Boot | 3.0.3 |
| 数据库 | PostgreSQL | 15 |
| 缓存 | Redis | 7 |
| 对象存储 | MinIO | 8.5.10 |
| 规则引擎 | Drools | 8.44.0.Final |
| API 文档 | Knife4j (OpenAPI 3) | 4.5.0 |
| 数据库迁移 | Flyway | 9.22.3 |
| 前端框架 | React + Umi Max | 18 / 4.6 |
| UI 组件 | Ant Design Pro Components | 5 |
| 图谱可视化 | AntV G6 | 5.0.10 |
| Node | Node.js | >= 18 |
| Mock 服务 | Python Flask | 3.x |

## 二、目录结构

```
SmartChain-main/
├── docker-compose.yml          # 一体化部署编排
├── readme.md                   # 本文档
├── docs/                       # 产品文档与 RFC
│   ├── prd/                    # 产品需求文档
│   └── spec/                   # 技术架构 RFC
├── scfs-backend/               # 后端多模块工程
│   ├── scfs-common/            # 通用层：安全、审计、实体、工具
│   ├── scfs-module-graph/      # 供应链图谱模块
│   ├── scfs-module-verify/     # 材料核验模块
│   ├── scfs-module-preaudit/   # 预审补正模块
│   ├── scfs-module-risk/       # 风险画像模块
│   └── scfs-app/               # 启动模块：配置、迁移脚本、Mapper XML
├── scfs-frontend/              # React 前端工程
│   ├── src/
│   │   ├── access/             # RBAC 权限守卫
│   │   ├── api/                # 接口封装
│   │   ├── components/         # 通用组件（图谱、上传、审批）
│   │   ├── pages/              # 业务页面
│   │   ├── routes.ts           # 路由与菜单配置
│   │   └── app.tsx             # 运行时入口
│   ├── Dockerfile              # 前端构建镜像
│   └── nginx.conf              # Nginx 配置（静态托管 + API 代理）
└── scfs-mock-server/           # 外部数据源 Mock 服务（Flask）
```

## 三、快速开始（Docker Compose）

### 1. 运行环境

- macOS、Linux 或 Windows + Docker Desktop
- Docker Desktop 已启动
- 至少 4 GB 可用内存，建议 8 GB
- 首次构建需要访问 Docker Hub、npm 镜像和 Maven 镜像

检查 Docker：

```bash
docker --version
docker compose version
docker info
```

### 2. 构建并启动

在项目根目录（包含 `docker-compose.yml`）执行：

```bash
docker compose up --build -d
```

首次构建会编译 Java 后端和 React 前端，时间可能较长。

### 3. 查看运行状态

```bash
docker compose ps
```

正常情况下应看到以下服务：

| 服务 | 用途 | 端口 |
| --- | --- | --- |
| `scfs-frontend` | 前端页面和 API 反向代理 | `80` |
| `scfs-app` | Spring Boot 后端 | `8080` |
| `scfs-postgres` | PostgreSQL 数据库 | `5432` |
| `scfs-redis` | Redis 缓存 | `6379` |
| `scfs-minio` | 文件对象存储 | `9000`、`9001` |
| `scfs-mock` | 外部数据源模拟服务 | `9002` |

### 4. 访问

本机访问：

```text
http://localhost/
```

局域网其他电脑访问时，先在部署机器上查看局域网 IP：

```bash
ifconfig
```

选择处于 `status: active` 网卡下的 IPv4 地址。例如本机 IP 为 `10.88.221.134`，则其他电脑访问 `http://10.88.221.134/`。不要使用 `127.0.0.1` 或 `localhost`，它们指向访问者自己的电脑。

## 四、默认账号与角色权限

### 1. 账号列表

所有种子用户密码统一为 `admin123`（BCrypt 哈希）。

| 用户 ID（初始化） | 用户名 | 密码 | 角色代码 | 角色名称 |
| ---: | --- | --- | --- | --- |
| 1 | `admin` | `admin123` | ADMIN | 系统管理员 |
| 2 | `rm_zhang` | `admin123` | RM | 客户经理 |
| 3 | `rco_li` | `admin123` | RCO | 风控审核员 |
| 4 | `maker_wang` | `admin123` | OPS_MAKER | 规则经办岗 |
| 5 | `checker_zhao` | `admin123` | OPS_CHECKER | 规则复核岗 |
| 6 | `ops_sun` | `admin123` | OPS | 运营主管 |
| 7 | `audit_zhou` | `admin123` | AUDIT | 审计人员 |

> 用户 ID 是 `schema_common.sys_user.id`，不是用户名。以上 ID 适用于按项目迁移脚本全新初始化的数据库；如果数据库曾经导入过其他数据，ID 可能不同，请以实际查询结果为准。分配审核人时，风控审核员 `rco_li` 默认使用用户 ID `3`。

### 2. 各角色可见菜单

| 菜单 | ADMIN | RM | RCO | OPS_MAKER | OPS_CHECKER | OPS | AUDIT |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 工作台 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| 供应链图谱 | ✅ | ✅ | ✅ | — | — | — | ✅ |
| 审核中心 | ✅ | ✅ | ✅ | — | — | — | — |
| 规则配置 | ✅ | — | — | ✅ | ✅ | ✅ | — |
| 审计查询 | ✅ | — | — | — | — | ✅ | ✅ |
| 系统管理 | ✅ | — | — | — | — | — | — |

### 3. 权限机制

- 后端通过 `@RequirePermission(module = "XXX", permission = "yyy")` 控制接口访问。
- 前端菜单根据登录返回的 `permissions` 字段动态渲染。
- module 名称大小写敏感，前端与后端必须一致（统一大写，如 `GRAPH`、`VERIFY`、`RULE`）。
- 如果某角色登录后只看到工作台，说明该角色缺少对应模块的 `view` 权限，需要检查 `V2__init_data.sql` 中 `sys_role_permission` 表是否为该角色分配了 `GRAPH.view`、`VERIFY.view` 等权限项。

### 4. 登录失败排查

- 登录返回 400 或"用户名或密码错误"：多半是数据库未初始化或旧密码哈希未更新，重建数据卷即可：

```bash
docker compose down -v
docker compose up --build -d
```

- 查看后端日志：

```bash
docker compose logs -f scfs-app
```

- 手动重置管理员密码（在 psql 内执行，避免 shell 转义问题）：

```bash
docker exec -it scfs-postgres psql -U scfs -d scfs_db -c \
  "UPDATE schema_common.sys_user SET password_hash='\$2a\$10\$EcoHpg3UNk5W9lq3SmgxoeHU9m9mDq/YXszNm4udwdcc/bEDzS7vS' WHERE username='admin';"
```

执行后使用 `admin/admin123` 登录。

## 五、本地开发

如果需要在本地直接运行前后端进行调试（不使用 Docker），参考以下方式。

### 1. 前端

前置条件：Node.js >= 18。

```bash
cd scfs-frontend
npm install
npm run dev
```

默认启动在 `http://localhost:8000`，通过 `.umirc.ts` 中的 proxy 将 `/api/v1` 代理到后端 `http://localhost:8080`。

### 2. 后端

前置条件：JDK 17、Maven 3.9+、本地 PostgreSQL（或连接 Docker 中的数据库）。

```bash
cd scfs-backend
mvn clean spring-boot:run -pl scfs-app
```

如果连接本地数据库，需在 `application.yml` 或环境变量中配置 `DB_HOST`、`DB_PASSWORD` 等。

### 3. Mock 服务

```bash
cd scfs-mock-server
pip install -r requirements.txt
python app.py
```

监听 9002 端口，提供 Mock OCR 接口。

## 六、供应链图谱

### 1. 功能说明

- 图谱页面路径：`/graph/relation`。
- 打开即展示全部企业，无需搜索。
- 默认使用力导向布局（force），支持自由拖动节点；可通过工具栏「径向布局 / 自由布局」按钮切换。
- 拖动节点后位置会被固定（`fx/fy`），如需重新布局点击「重新加载」。
- 前端基于 AntV G6 v5，使用 `behaviors` 配置（非旧版 `modes`），依赖 `@antv/g6@^5.0.10`。

### 2. 后端接口

| 接口 | 用途 |
| --- | --- |
| `GET /api/v1/graph/full` | 全量企业关系 |
| `GET /api/v1/graph/roles` | 全量企业角色 |
| `GET /api/v1/graph/positions` | 全量位置分析 |
| `GET /api/v1/graph/abnormals` | 全量异常预警 |

### 3. 企业角色 / 位置分析 / 异常预警

- 三个页面分别位于 `/graph/role`、`/graph/position`、`/graph/abnormal`。
- 均为加载即展示全量数据，支持搜索过滤。
- 后端实体类 `EnterpriseRole`、`EnterprisePositionAnalysis`、`AbnormalRelation` 已包含 `enterpriseName` 字段，`GraphMapper.xml` 的 resultMap 已映射 `enterprise_name`。如果修改了实体或 SQL，务必同步更新 resultMap，否则企业名称列会空白。

## 七、初始化注意事项

首次拉起本项目时，除了执行 `docker compose up --build -d` 外，还需要关注以下要点。

### 1. 数据库种子数据

项目通过 Flyway 在后端启动时自动执行迁移脚本，位于：

```
scfs-backend/scfs-app/src/main/resources/db/migration/
├── V1__init_schema.sql      # 建表
├── V2__init_data.sql        # 角色、权限、菜单、用户
└── V3__seed_data.sql        # 企业、关系、角色、位置分析、异常预警
```

- 首次启动 `scfs-app` 会自动执行，无需手动运行。
- Flyway 迁移脚本一经执行不会重跑。如果只改了 SQL 但库已存在，需要重建数据卷：

```bash
docker compose down -v
docker compose up --build -d
```

### 2. 前端构建缓存

修改前端代码后，如果浏览器出现 `ChunkLoadError` 或加载到旧 chunk，需要：

```bash
docker compose build scfs-frontend --no-cache
docker compose up -d scfs-frontend
```

并在浏览器中强制刷新（macOS：`Command + Shift + R`，Windows/Linux：`Ctrl + Shift + R`）。

Nginx 对静态资源设置了 7 天缓存（`immutable`），修改后必须重建镜像才能生效。

### 3. 后端重建

修改 Java 代码或 SQL 后：

```bash
docker compose build scfs-app
docker compose up -d scfs-app
```

## 八、常用命令

查看全部日志：

```bash
docker compose logs -f
```

只查看后端日志：

```bash
docker compose logs -f scfs-app
```

只查看前端日志：

```bash
docker compose logs -f scfs-frontend
```

停止服务但保留数据：

```bash
docker compose down
```

重新构建并启动全部：

```bash
docker compose up --build -d
```

只重新构建前端：

```bash
docker compose build scfs-frontend
docker compose up -d scfs-frontend
```

只重新构建后端：

```bash
docker compose build scfs-app
docker compose up -d scfs-app
```

## 九、常见问题

### 1. Docker Hub 拉取镜像超时

例如 `failed to fetch oauth token` 或 `DeadlineExceeded`。可以先单独拉取镜像，确认网络恢复后再构建：

```bash
docker pull node:18-alpine
docker pull nginx:1.25-alpine
docker pull maven:3.9-eclipse-temurin-17
docker pull postgres:15
docker pull redis:7-alpine
docker pull minio/minio
```

### 2. 端口冲突

如果出现 `Bind for 0.0.0.0:80 failed: port is already allocated`，说明本机 80 端口被占用。查看占用情况：

```bash
lsof -nP -iTCP:80 -sTCP:LISTEN
docker ps
```

将 `docker-compose.yml` 中前端端口改为 `8088:80`，重新启动后访问 `http://localhost:8088/`。

### 3. 前端页面空白

依次执行：

```bash
docker compose build scfs-frontend --no-cache
docker compose up -d scfs-frontend
```

然后浏览器强制刷新。

### 4. 页面请求返回 500

查看后端日志：

```bash
docker compose logs --tail=200 scfs-app
```

同时确认后端容器没有反复重启：

```bash
docker compose ps
```

### 5. 局域网无法访问

确认以下条件：

1. 两台电脑连接同一网络。
2. 访问的是部署机器的局域网 IP，而不是 `localhost`。
3. 前端容器端口映射为 `0.0.0.0:80->80/tcp`。
4. macOS 防火墙没有阻止 Docker Desktop 或端口 80。
5. 如果使用 VPN，暂时关闭 VPN 后重试。

## 十、数据与安全说明

PostgreSQL、Redis、MinIO 和 Mock 服务使用 Docker named volume 保存数据。执行 `docker compose down` 会停止容器，但不会删除这些数据。

不要在生产环境继续使用文档中的默认密码、数据库密码和 JWT 密钥。生产部署前应修改 `docker-compose.yml` 中的：

- `POSTGRES_PASSWORD`
- `MINIO_ROOT_PASSWORD`
- `DB_PASSWORD`
- `MINIO_SECRET_KEY`
- `JWT_SECRET`
- `SCFS_SECURITY_JWT_SECRET`

删除数据属于破坏性操作，请不要随意执行：

```bash
docker compose down -v
```

该命令会删除 Compose 管理的数据库、Redis、MinIO 和 Mock 数据卷。
