#!/bin/sh
set -e

# Render tự động cấp phát biến môi trường $PORT (mặc định 10000 trên Render, fallback về 8080)
TARGET_PORT=${PORT:-8080}
echo "[Tomcat] Binding HTTP connector to port: $TARGET_PORT"

# Vô hiệu hóa cổng shutdown 8005 (tránh Render scanner gửi HTTP probe vào port socket shutdown gây spam log)
sed -i 's/<Server port="8005"/<Server port="-1"/g' /usr/local/tomcat/conf/server.xml

# Cập nhật cổng lắng nghe HTTP trong server.xml của Tomcat
sed -i "s/port=\"8080\"/port=\"$TARGET_PORT\"/g" /usr/local/tomcat/conf/server.xml

# Khởi chạy Tomcat
exec catalina.sh run
