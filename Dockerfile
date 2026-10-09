# ==========================================
# DOCKERFILE CHO HỆ THỐNG QUẢN LÝ SÂN CẦU LÔNG
# ==========================================
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Copy tệp jar đã đóng gói
COPY target/Badminton_Management_Project-1.0.jar app.jar

# Mở cổng 8080
EXPOSE 8080

# Cấu hình biến môi trường mặc định
ENV SPRING_PROFILES_ACTIVE=prod
ENV PORT=8080

# Khởi chạy ứng dụng Spring Boot
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
