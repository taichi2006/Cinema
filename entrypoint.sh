#!/bin/sh
set -e

# Render tự động cấp phát biến môi trường $PORT (mặc định 10000 trên Render, fallback về 8080)
TARGET_PORT=${PORT:-8080}
echo "[Tomcat] Binding HTTP connector to port: $TARGET_PORT"

# Cập nhật cổng lắng nghe trong server.xml của Tomcat
sed -i "s/port=\"8080\"/port=\"$TARGET_PORT\"/g" /usr/local/tomcat/conf/server.xml

# Khởi chạy Tomcat
exec catalina.sh run
