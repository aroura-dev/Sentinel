#!/bin/bash

BASE_DIR="$(cd "$(dirname "$0")" && pwd)"
JAR_PATH="$BASE_DIR/sentinel-stream/target/sentinel-stream-0.0.1-SNAPSHOT.jar"
if [[ ! -f "$JAR_PATH" ]]; then
    echo "jar 不存在: $JAR_PATH" >&2
        exit 1
fi

docker cp "$JAR_PATH" sentinel-jobmanager-1:/opt/sentinel-stream-0.0.1-SNAPSHOT.jar
docker exec -ti sentinel-jobmanager-1 flink run /opt/sentinel-stream-0.0.1-SNAPSHOT.jar

# stream local test
# docker cp ./sentinel-stream-0.0.1-SNAPSHOT.jar sentinel_jobmanager_1:/opt/sentinel-stream-test-0.0.1-SNAPSHOT.jar
# docker exec -ti sentinel_jobmanager_1 flink run /opt/sentinel-stream-test-0.0.1-SNAPSHOT.jar


# data-house
# ./flink run sentinel-data-house-0.0.1-SNAPSHOT.jar

