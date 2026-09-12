package com.aroura.sentinel.stream.utils;

import com.aroura.sentinel.stream.callback.RedisPipelineCallBack;
import com.aroura.sentinel.stream.constants.SentinelFlinkConstant;
import io.lettuce.core.LettuceFutures;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.codec.ByteArrayCodec;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author Sentinel
 * @date 2022/2/22
 * 无Spring环境下使用Redis，基于Lettuce封装
 */
public class LettuceRedisUtils {

    /**
     * 初始化 redisClient
     */
    private static final RedisClient REDIS_CLIENT;

    static {
        RedisURI redisUri = RedisURI.Builder.redis(SentinelFlinkConstant.REDIS_IP)
                .withPort(Integer.parseInt(SentinelFlinkConstant.REDIS_PORT))
                .withPassword(SentinelFlinkConstant.REDIS_PASSWORD.toCharArray())
                .build();
        REDIS_CLIENT = RedisClient.create(redisUri);
    }

    private LettuceRedisUtils() {

    }

    /**
     * 封装pipeline操作
     */
    public static void pipeline(RedisPipelineCallBack pipelineCallBack) {
        StatefulRedisConnection<byte[], byte[]> connect = REDIS_CLIENT.connect(new ByteArrayCodec());
        RedisAsyncCommands<byte[], byte[]> commands = connect.async();

        List<RedisFuture<?>> futures = pipelineCallBack.invoke(commands);

        commands.flushCommands();
        LettuceFutures.awaitAll(10, TimeUnit.SECONDS,
                futures.toArray(new RedisFuture[0]));
        connect.close();
    }

}
