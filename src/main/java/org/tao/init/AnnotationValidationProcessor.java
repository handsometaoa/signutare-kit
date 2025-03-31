package org.tao.init;

import com.alibaba.fastjson2.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.tao.anno.SignatureChecker;
import org.tao.anno.SignatureParam;
import org.tao.config.SignatureProperties;
import org.tao.consts.SignatureConst;
import org.tao.enums.SignatureParamTypeEnum;
import org.tao.exception.GlobalExceptionHandler;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;

@Component
public class AnnotationValidationProcessor implements BeanPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Resource
    private SignatureProperties signatureProperties;

    @PostConstruct
    public void preCheck() {
        Map<String, String> returnJsons = signatureProperties.getReturnJsons();
        if (returnJsons != null && !returnJsons.isEmpty()) {
            returnJsons.forEach((key, value) -> {
                if (!JSON.isValidObject(value)) {
                    log.warn("[验签配置校验] 返回类型 json 字符串配置错误，value => {}", value);
                    throw new IllegalStateException("[验签配置校验] 返回类型 json 字符串配置错误，请检查配置！");
                }
            });
        }
    }


    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        Class<?> clazz = bean.getClass();
        for (Method method : clazz.getDeclaredMethods()) {
            // 检查方法是否带有 @SignatureChecker
            if (method.isAnnotationPresent(SignatureChecker.class)) {
                SignatureChecker annotation = method.getAnnotation(SignatureChecker.class);

                if (!StringUtils.isEmpty(annotation.serviceCode()) && StringUtils.isEmpty(annotation.secretKey())) {
                    if (!signatureProperties.getSecretKeys().containsKey(annotation.serviceCode())) {
                        log.warn("[验签配置校验] 配置缺失服务编码对应的密钥，serviceCode => {}", annotation.serviceCode());
                        throw new IllegalStateException("[验签配置校验] 配置缺失服务编码对应的密钥，请检查配置！");
                    }
                }
                if (!annotation.returnType().equals(SignatureConst.DEFAULT_RETURN_TYPE)) {
                    if (!signatureProperties.getReturnJsons().containsKey(annotation.returnType())) {
                        log.warn("[验签配置校验] 配置缺失返回类型 JSON 字符串，returnType => {}", annotation.returnType());
                        throw new IllegalStateException("[验签配置校验] 配置缺失返回类型 JSON 字符串，请检查配置！");
                    }
                }

                // 检查是否同时带有 @PostMapping 或 @RequestMapping
                if (!method.isAnnotationPresent(PostMapping.class) && !method.isAnnotationPresent(RequestMapping.class)) {
                    throw new IllegalStateException("方法 " + method.getName() + " 必须同时带有 @PostMapping 或 @RequestMapping 注解");
                }
                // 检查方法参数
                validateMethodParameters(method);
            }
        }
        return bean;
    }

    private void validateMethodParameters(Method method) {
        // 这里可以根据业务需求检查方法的参数
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length == 0) {
            throw new IllegalStateException("[验签配置校验] 方法 " + method.getClass().getName() + "." + method.getName() + " 必须至少有一个参数");
        }
        // 其他校验逻辑
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        for (int i = 0; i < parameterAnnotations.length; i++) {
            for (Annotation anno : parameterAnnotations[i]) {
                if (anno instanceof SignatureParam) {
                    SignatureParam signatureParam = (SignatureParam) anno;
                    Class<?> aClass = parameterTypes[i];
                    if (signatureParam.type() == SignatureParamTypeEnum.PARAMS) {
                        if (isPrimitiveType(aClass)) {
                            throw new IllegalStateException("[验签配置校验] " + method.getName() + " 方法 params 参数" + " 必须是对象类型");
                        }
                    } else if (signatureParam.type() == SignatureParamTypeEnum.SERVICE_CODE) {
                        if (aClass != String.class) {
                            throw new IllegalStateException("[验签配置校验] " + method.getName() + " 方法 serviceCode 参数" + " 必须是String类型");
                        }
                    } else {
                        throw new IllegalStateException("[验签配置校验] " + method.getName() + " 暂未适配类型");
                    }
                }
            }
        }
    }

    private boolean isPrimitiveType(Class<?> clazz) {
        return clazz.isPrimitive() || clazz == Integer.class || clazz == Boolean.class ||
                clazz == Character.class || clazz == Byte.class || clazz == Short.class ||
                clazz == Long.class || clazz == Float.class || clazz == Double.class;
    }

}
