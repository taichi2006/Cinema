# ===================================================
# Stage 1: Build WAR application with Maven & JDK 21
# ===================================================
FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /app

# Copy pom.xml và tải dependencies (cache layer)
COPY pom.xml ./
RUN mvn dependency:go-offline -B || true

# Copy toàn bộ mã nguồn
COPY src ./src

# Build file WAR (bỏ qua unit test khi build image)
RUN mvn clean package -DskipTests -B

# ===================================================
# Stage 2: Runtime với Tomcat 10.1 & JRE 21
# ===================================================
FROM tomcat:10.1-jre21-temurin

# Dọn dẹp các webapp mặc định của Tomcat
RUN rm -rf /usr/local/tomcat/webapps/*

# Copy file WAR đã build vào thư mục webapps dưới tên ROOT.war
# Ứng dụng sẽ chạy ở context path "/" (không cần prefix context)
COPY --from=builder /app/target/*.war /usr/local/tomcat/webapps/ROOT.war

# Copy script khởi động và chuyển đổi CRLF sang LF (tránh lỗi trên Windows)
COPY entrypoint.sh /usr/local/tomcat/bin/entrypoint.sh
RUN tr -d '\r' < /usr/local/tomcat/bin/entrypoint.sh > /usr/local/tomcat/bin/entrypoint-clean.sh \
    && mv /usr/local/tomcat/bin/entrypoint-clean.sh /usr/local/tomcat/bin/entrypoint.sh \
    && chmod +x /usr/local/tomcat/bin/entrypoint.sh

# Cấu hình tối ưu bộ nhớ cho Render Free Tier (512MB RAM)
ENV JAVA_OPTS="-Xms128m -Xmx320m -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError"
ENV PORT=8080

EXPOSE 8080

CMD ["/usr/local/tomcat/bin/entrypoint.sh"]
