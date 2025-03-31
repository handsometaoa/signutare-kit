package org.tao.anno;

import org.tao.consts.SignatureConst;
import org.tao.enums.SignatureParamTypeEnum;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface SignatureParam {

    // 0:标识serviceCode 1:标识请求参数
    SignatureParamTypeEnum type() default SignatureParamTypeEnum.PARAMS;

    String requestIdField() default SignatureConst.EMPTY_STR;

    String timestampField() default SignatureConst.EMPTY_STR;

    String signatureField() default SignatureConst.EMPTY_STR;

}
