# SCFS 供应链金融风控平台部署文档

本文档适用于使用 Docker Compose 在本机或局域网部署 SCFS 平台。

## 一、运行环境

- macOS、Linux 或 Windows + Docker Desktop
- Docker Desktop 已启动
- 至少 4 GB 可用内存，建议 8 GB
- 首次构建需要访问 Docker Hub、npm 镜像和 Maven 镜像
- 局域网访问时，部署机器和访问电脑必须处于同一局域网

检查 Docker：

```bash
docker --version
docker compose version
docker info
```

## 二、项目目录

在包含 `docker-compose.yml` 的项目根目录执行命令：

```bash
cd /Users/crawler/workPlace/traeTest/SmartChain-main/SmartChain-main
```

如果项目位于其他目录，请将上面的路径替换为实际路径。

## 三、首次启动

### 1. 启动 Docker Desktop

如果出现以下错误，说明 Docker 服务未启动：

```text
Cannot connect to the Docker daemon
```

请先打开 Docker Desktop，等待 Docker 状态变为 Running。

### 2. 构建并启动全部服务

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

## 四、访问地址

### 本机访问

```text
http://localhost/
```

登录页面：

```text
http://localhost/login
```

### 局域网其他电脑访问

先在部署机器上查看局域网 IP：

```bash
ifconfig
```

选择处于 `status: active` 网卡下的 IPv4 地址。例如本机 IP 为 `10.88.221.134`，则其他电脑访问：

```text
http://10.88.221.134/
```

登录页面：

```text
http://10.88.221.134/login
```

不要使用 `127.0.0.1` 或 `localhost`，它们指向访问者自己的电脑。

## 五、默认账号

初始化数据中默认账号为：

```text
用户名：admin
密码：admin123
```

如果数据库已经初始化过，重新执行 `docker compose up` 不会覆盖已有用户数据。登录失败时，可查看后端日志：

```bash
docker compose logs -f scfs-app
```

仅用于本地开发环境时，可以重置管理员密码：

```bash
docker exec -i scfs-postgres psql -U scfs -d scfs_db \
  -c "UPDATE schema_common.sys_user SET password_hash='\$2a\$10\$EcoHpg3UNk5W9lq3SmgxoeHU9m9mDq/YXszNm4udwdcc/bEDzS7vS' WHERE username='admin';"
```

执行后使用 `admin/admin123` 登录。

## 六、常用命令

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

停止服务但保留数据库数据：

```bash
docker compose down
```

重新构建并启动：

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

## 七、端口冲突处理

如果出现：

```text
Bind for 0.0.0.0:80 failed: port is already allocated
```

说明本机 80 端口已经被其他程序或容器占用。查看占用情况：

```bash
lsof -nP -iTCP:80 -sTCP:LISTEN
docker ps
```

如果是其他 Nginx、Dify 或旧项目占用，需要停止对应容器，或者将 `docker-compose.yml` 中前端端口改为：

```yaml
ports:
  - "8088:80"
```

改完后重新启动：

```bash
docker compose up --build -d
```

此时访问地址变为：

```text
http://localhost:8088/
```

局域网访问则为：

```text
http://<部署机器局域网IP>:8088/
```

## 八、常见问题

### 1. Docker Hub 拉取镜像超时

例如：

```text
failed to fetch oauth token
DeadlineExceeded
```

这是 Docker 镜像仓库网络连接问题。可以先单独拉取镜像，确认网络恢复后再构建：

```bash
docker pull node:18-alpine
docker pull nginx:1.25-alpine
docker pull maven:3.9-eclipse-temurin-17
docker pull postgres:15
docker pull redis:7-alpine
docker pull minio/minio
```

### 2. 前端页面空白

依次执行：

```bash
docker compose build scfs-frontend
docker compose up -d scfs-frontend
```

然后在浏览器执行强制刷新：

- macOS：`Command + Shift + R`
- Windows/Linux：`Ctrl + Shift + R`

### 3. 页面请求返回 500

查看后端日志：

```bash
docker compose logs --tail=200 scfs-app
```

同时确认后端容器没有反复重启：

```bash
docker compose ps
```

### 4. 局域网无法访问

确认以下条件：

1. 两台电脑连接同一网络。
2. 访问的是部署机器的局域网 IP，而不是 `localhost`。
3. 前端容器端口映射为 `0.0.0.0:80->80/tcp`。
4. macOS 防火墙没有阻止 Docker Desktop 或端口 80。
5. 如果使用 VPN，暂时关闭 VPN 后重试。

## 九、数据与安全说明

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
