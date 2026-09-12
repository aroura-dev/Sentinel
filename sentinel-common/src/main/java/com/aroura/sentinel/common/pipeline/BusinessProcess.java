package com.aroura.sentinel.common.pipeline;

/**
 * 业务执行器
 *
 * @author Sentinel
 */
public interface BusinessProcess<T extends ProcessModel> {

    /**
     * 真正处理逻辑
     *
     * @param context
     */
    void process(ProcessContext<T> context);
}
