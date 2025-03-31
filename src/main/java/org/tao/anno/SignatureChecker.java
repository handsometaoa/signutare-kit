package org.tao.anno;

import org.tao.consts.SignatureConst;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SignatureChecker {

    // 服务Code
    String serviceCode() default SignatureConst.EMPTY_STR;

    // 签名生成密钥
    String secretKey() default SignatureConst.EMPTY_STR;

    // 签名过期时间，单位为分钟
    int expireMinutes() default -1;

    // 默认为true，表示需要验证签名
    boolean required() default true;

    // 返回值类型
    String returnType() default SignatureConst.DEFAULT_RETURN_TYPE;

}