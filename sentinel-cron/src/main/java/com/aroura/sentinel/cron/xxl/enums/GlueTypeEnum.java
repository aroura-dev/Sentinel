package com.aroura.sentinel.cron.xxl.enums;

/**
 * GlueTyp 类型（默认BEAN)
 *
 * @author Sentinel
 */
public enum GlueTypeEnum {

    /**
     * BEAN
     */
    BEAN,
    /**
     * GLUE_GROOVY
     */
    GLUE_GROOVY,
    /**
     * GLUE_SHELL
     */
    GLUE_SHELL,
    /**
     * GLUE_PHP
     */
    GLUE_PHP,
    /**
     * GLUE_NODEJS
     */
    GLUE_NODEJS,
    /**
     * GLUE_POWERSHELL
     */
    GLUE_POWERSHELL;

    GlueTypeEnum() {
    }
}
