package com.aroura.sentinel.support.mq.springeventbus;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * @author Sentinel
 */
@Data
@Builder
public class SentinelSpringEventSource implements Serializable {
    private String topic;
    private String jsonValue;
    private String tagId;
}
