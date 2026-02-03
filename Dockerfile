# ========================================
# 多階段建構 Dockerfile
# ========================================

# 階段一：建構
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

WORKDIR /app

# 複製 pom.xml 並下載依賴（利用 Docker 快取）
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 複製原始碼並建構
COPY src ./src
RUN mvn package -DskipTests -B

# 階段二：執行
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 安裝中文字型（用於 Rich Menu 圖片生成）
# font-wqy-zenhei: 文泉驛正黑（開源中文字型，支援繁簡中文）
RUN apk add --no-cache \
    fontconfig \
    ttf-dejavu \
    font-wqy-zenhei \
    && fc-cache -fv

# 建立非 root 用戶
RUN addgroup -g 1001 appgroup && \
    adduser -u 1001 -G appgroup -D appuser

# 複製 JAR 檔案
COPY --from=builder /app/target/*.jar app.jar

# 設定權限
RUN chown -R appuser:appgroup /app
USER appuser

# 設定環境變數
ENV JAVA_OPTS="-Xms256m -Xmx512m"
ENV SPRING_PROFILES_ACTIVE=prod

# 暴露埠號
EXPOSE 8080

# 健康檢查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# 啟動應用程式
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
