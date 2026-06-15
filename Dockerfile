FROM eclipse-temurin:8-jre

# 时区
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# JVM 参数
ENV JAVA_OPTS="-Xmx256m -Xms256m -Djava.security.egd=file:/dev/./urandom"

# 构建参数（在 docker-compose 中传入）
ARG SERVICE_NAME
ARG SERVICE_VERSION=1.0.0

LABEL service=${SERVICE_NAME}

# 复制预构建的 jar
COPY ${SERVICE_NAME}/target/${SERVICE_NAME}-${SERVICE_VERSION}.jar /app.jar

# 启动（SPRING_PROFILES_ACTIVE 由 docker-compose 环境变量传入）
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar /app.jar"]
