# Sentinel 后端镜像
# 说明：本地/演示用 dev profile（eventBus MQ，无需 Kafka/Nacos/XXL）。
# 原 Dockerfile 用 test profile（需整套 austin 基建），与本地 dev 不一致，已修正。
FROM eclipse-temurin:8-jre

ENV PARAMS="--spring.profiles.active=dev" \
    DASHSCOPE_API_KEY="" \
    AUSTIN_DATABASE_IP=127.0.0.1 \
    AUSTIN_DATABASE_PORT=3306 \
    AUSTIN_REDIS_IP=127.0.0.1 \
    AUSTIN_REDIS_PORT=6379 \
    AUSTIN_REDIS_PASSWORD=austin

WORKDIR /build

COPY ./austin-web/target/austin-web-0.0.1-SNAPSHOT.jar ./austin.jar

EXPOSE 8080

ENTRYPOINT ["sh","-c","java -jar $JAVA_OPTS austin.jar $PARAMS --austin.database.ip=$AUSTIN_DATABASE_IP --austin.database.port=$AUSTIN_DATABASE_PORT --austin.redis.ip=$AUSTIN_REDIS_IP --austin.redis.port=$AUSTIN_REDIS_PORT --austin.redis.password=$AUSTIN_REDIS_PASSWORD"]
