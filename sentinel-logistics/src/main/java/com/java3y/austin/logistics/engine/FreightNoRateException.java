package com.java3y.austin.logistics.engine;

/**
 * 运费价卡未命中异常
 *
 * @author sentinel
 */
public class FreightNoRateException extends RuntimeException {

    public FreightNoRateException(String message) {
        super(message);
    }
}
