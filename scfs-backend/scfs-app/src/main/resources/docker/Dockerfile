# 后端镜像 - 多阶段构建（对应 RFC 6.2 阶段 0 S0-5）
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build
COPY .mvn/settings.xml /root/.m2/settings.xml
COPY . .
RUN mvn clean package -DskipTests

# 运行阶段
# 本地开发阶段复用已下载的 Maven/JDK 镜像，避免额外拉取运行时镜像。
# 生产环境可替换为 eclipse-temurin:17-jre 等更精简的 JRE 镜像。
FROM maven:3.9-eclipse-temurin-17
LABEL maintainer="SCFS Team"
LABEL description="SCFS Supply Chain Finance Risk Control Platform"

WORKDIR /app
ENV TZ=Asia/Shanghai
ENV JAVA_OPTS="-Xms512m -Xmx2g -XX:+UseG1GC -XX:+HeapDumpOnOutOfMemoryError"

COPY --from=builder /build/scfs-app/target/scfs-app.jar /app/scfs-app.jar

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/scfs-app.jar"]
