# 后端镜像 - 多阶段构建（对应 RFC 6.2 阶段 0 S0-5）
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build
COPY . .
RUN mvn clean package -DskipTests -q

# 运行阶段
FROM openjdk:17-slim
LABEL maintainer="SCFS Team"
LABEL description="SCFS Supply Chain Finance Risk Control Platform"

WORKDIR /app
ENV TZ=Asia/Shanghai
ENV JAVA_OPTS="-Xms512m -Xmx2g -XX:+UseG1GC -XX:+HeapDumpOnOutOfMemoryError"

COPY --from=builder /build/scfs-app/target/scfs-app.jar /app/scfs-app.jar

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/scfs-app.jar"]
