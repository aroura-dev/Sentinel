# Sentinel 后端镜像：容器内自动构建，无需宿主机预装 Maven/JDK。
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN mvn -B -ntp -DskipTests -pl sentinel-web -am package

FROM eclipse-temurin:8-jre

ENV PARAMS="--spring.profiles.active=dev" \
    DASHSCOPE_API_KEY="" \
    SENTINEL_DATABASE_IP=mysql \
    SENTINEL_DATABASE_PORT=3306 \
    SENTINEL_DATABASE_PASSWORD="" \
    SENTINEL_REDIS_IP=redis \
    SENTINEL_REDIS_PORT=6379 \
    SENTINEL_REDIS_PASSWORD=sentinel

WORKDIR /app
COPY --from=build /workspace/sentinel-web/target/sentinel-web-0.0.1-SNAPSHOT.jar /app/sentinel.jar

EXPOSE 8080
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/sentinel.jar $PARAMS --sentinel.database.ip=$SENTINEL_DATABASE_IP --sentinel.database.port=$SENTINEL_DATABASE_PORT --sentinel.database.password=$SENTINEL_DATABASE_PASSWORD --sentinel.redis.ip=$SENTINEL_REDIS_IP --sentinel.redis.port=$SENTINEL_REDIS_PORT --sentinel.redis.password=$SENTINEL_REDIS_PASSWORD"]