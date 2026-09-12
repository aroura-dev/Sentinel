package com.aroura.sentinel.stream;

import com.aroura.sentinel.common.domain.AnchorInfo;
import com.aroura.sentinel.stream.constants.SentinelFlinkConstant;
import com.aroura.sentinel.stream.function.SentinelFlatMapFunction;
import com.aroura.sentinel.stream.sink.SentinelSink;
import com.aroura.sentinel.stream.utils.MessageQueueUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

/**
 * flink启动类
 *
 * @author Sentinel
 */
@Slf4j
public class SentinelBootStrap {

    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        /**
         * 1.获取KafkaConsumer
         */
        KafkaSource<String> kafkaConsumer = MessageQueueUtils.getKafkaConsumer(SentinelFlinkConstant.TOPIC_NAME, SentinelFlinkConstant.GROUP_ID, SentinelFlinkConstant.BROKER);
        DataStreamSource<String> kafkaSource = env.fromSource(kafkaConsumer, WatermarkStrategy.noWatermarks(), SentinelFlinkConstant.SOURCE_NAME);


        /**
         * 2. 数据转换处理
         */
        SingleOutputStreamOperator<AnchorInfo> dataStream = kafkaSource.flatMap(new SentinelFlatMapFunction()).name(SentinelFlinkConstant.FUNCTION_NAME);

        /**
         * 3. 将实时数据多维度写入Redis(已实现)，离线数据写入hive(未实现)
         */
        dataStream.addSink(new SentinelSink()).name(SentinelFlinkConstant.SINK_NAME);
        env.execute(SentinelFlinkConstant.JOB_NAME);

    }

}
