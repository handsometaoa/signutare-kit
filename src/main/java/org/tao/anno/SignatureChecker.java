package org.tao.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD) // 注解只能用于方法
@Retention(RetentionPolicy.RUNTIME) // 注解在运行时可见
public @interface SignatureChecker {
    
    // 服务Code
    String serviceCode();
    // 密钥
    String secretKey() default "";
    // 默认为true，表示需要验证签名
    boolean required() default true;
    // 过期时间，单位为分钟
    int expireMinutes() default -1;
    
}
