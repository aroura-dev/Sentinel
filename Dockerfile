# Sentinel 后端镜像
# 说明：本地/演示用 dev profile（eventBus MQ，无需 Kafka/Nacos/XXL）。
# 原 Dockerfile 用 test profile（需整套 sentinel 基建），与本地 dev 不一致，已修正。
FROM eclipse-temurin:8-jre

ENV PARAMS="--spring.profiles.active=dev" \
    DASHSCOPE_API_KEY="" \
    SENTINEL_DATABASE_IP=127.0.0.1 \
    SENTINEL_DATABASE_PORT=3306 \
    SENTINEL_DATABASE_PASSWORD="" \
    SENTINEL_REDIS_IP=127.0.0.1 \
    SENTINEL_REDIS_PORT=6379 \
    SENTINEL_REDIS_PASSWORD=sentinel

WORKDIR /build

COPY ./sentinel-web/target/sentinel-web-0.0.1-SNAPSHOT.jar ./sentinel.jar

EXPOSE 8080

ENTRYPOINT ["sh","-c","java -jar $JAVA_OPTS sentinel.jar $PARAMS --sentinel.database.ip=$SENTINEL_DATABASE_IP --sentinel.database.port=$SENTINEL_DATABASE_PORT --sentinel.database.password=$SENTINEL_DATABASE_PASSWORD --sentinel.redis.ip=$SENTINEL_REDIS_IP --sentinel.redis.port=$SENTINEL_REDIS_PORT --sentinel.redis.password=$SENTINEL_REDIS_PASSWORD"]
