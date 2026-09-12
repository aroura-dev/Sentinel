package com.aroura.sentinel.stream.function;

import com.alibaba.fastjson2.JSON;
import com.aroura.sentinel.common.domain.AnchorInfo;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.util.Collector;

/**
 * @author Sentinel
 * @date 2022/2/22
 * process 处理
 */
public class SentinelFlatMapFunction implements FlatMapFunction<String, AnchorInfo> {

    @Override
    public void flatMap(String value, Collector<AnchorInfo> collector){
        AnchorInfo anchorInfo = JSON.parseObject(value, AnchorInfo.class);
        collector.collect(anchorInfo);
    }
}
