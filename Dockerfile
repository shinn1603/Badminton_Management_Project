# ==============================================================
# GIAI ĐOẠN 1: BUILD ỨNG DỤNG TỪ SOURCE CODE (MULTI-STAGE BUILD)
# Tương thích 100% với CI/CD, Render.com, Railway.app & GitHub
# ==============================================================
FROM maven:3.9.6-eclipse-temurin-21-jammy AS build
WORKDIR /app

# Cache dependency layer để tăng tốc độ build các lần sau
COPY pom.xml .
RUN mvn dependency:go-offline -B || true

# Copy toàn bộ mã nguồn vào và đóng gói file JAR
COPY src ./src
RUN mvn clean package -DskipTests

# ==============================================================
# GIAI ĐOẠN 2: RUNTIME CONTAINER SIÊU NHẸ (ECLIPSE TEMURIN JRE 21)
# ==============================================================
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Copy file jar đã build từ giai đoạn 1
COPY --from=build /app/target/Badminton_Management_Project-*.jar app.jar

# Khai báo port (mặc định 8080, sẽ nhận PORT động từ Render/Railway)
EXPOSE 8080

# Cấu hình biến môi trường mặc định
ENV SPRING_PROFILES_ACTIVE=prod
ENV PORT=8080

# Tối ưu RAM cho Cloud Hosting (Render 512MB / Railway):
# -XX:+UseContainerSupport nhận diện giới hạn RAM của container
# -XX:MaxRAMPercentage=75.0 giới hạn Heap tránh lỗi OOM Killed
ENTRYPOINT ["sh", "-c", "java -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom -Dserver.port=${PORT:-8080} -jar app.jar"]
