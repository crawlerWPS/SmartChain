# 多阶段构建：node 构建 + nginx 托管
FROM node:18-alpine AS builder
WORKDIR /app
COPY package.json ./
RUN npm config set registry https://registry.npmmirror.com && npm install --no-audit --no-fund
COPY . .
RUN npm run build

FROM nginx:latest
LABEL maintainer="SCFS Team"
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
HEALTHCHECK --interval=30s --timeout=3s CMD curl --fail --silent http://localhost/healthz > /dev/null || exit 1
CMD ["nginx", "-g", "daemon off;"]
