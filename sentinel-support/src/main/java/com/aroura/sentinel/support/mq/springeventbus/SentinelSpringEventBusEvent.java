package com.aroura.sentinel.support.mq.springeventbus;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 描述：消息
 *
 * @author tony
 * @date 2023/2/6 19:59
 */
@Getter
public class SentinelSpringEventBusEvent extends ApplicationEvent {

    private final SentinelSpringEventSource sentinelSpringEventSource;

    public SentinelSpringEventBusEvent(Object source, SentinelSpringEventSource sentinelSpringEventSource) {
        super(source);
        this.sentinelSpringEventSource = sentinelSpringEventSource;
    }

}
