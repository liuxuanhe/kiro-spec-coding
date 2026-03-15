package com.parking.common;

import java.lang.annotation.*;

/**
 * 操作日志注解，标注在需要记录操作日志的方法上
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLogAnnotation {

    /** 操作类型，如 CREATE、UPDATE、DELETE、AUDIT */
    String operationType();

    /** 操作目标类型，如 owner、vehicle、visitor */
    String targetType();
}
