package org.tao.aspect;

import com.alibaba.fastjson2.JSON;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.tao.anno.SignatureChecker;
import org.tao.anno.SignatureParam;
import org.tao.config.SignatureProperties;
import org.tao.exception.SignatureValidationException;
import org.tao.utils.SignatureUtil;

import javax.annotation.Resource;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;

@Aspect
@Component
public class SignatureAspect {
    private static final Logger logger = LoggerFactory.getLogger(SignatureAspect.class);

    @Resource
    private SignatureProperties signatureProperties;

    @Around("@annotation(org.tao.anno.SignatureChecker)")
    public Object validateSignature(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        // 获取方法的所有参数
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        Object[] args = joinPoint.getArgs();

        Map<String, Object> paramMap = null;
        SignatureChecker signatureChecker = method.getAnnotation(SignatureChecker.class);
        if (signatureChecker != null && signatureChecker.required()) {
            SignatureParam signatureParam = null;
            // 遍历参数注解，找到被 @SignatureParam 注解修饰的参数
            outerLoop:
            for (int i = 0; i < parameterAnnotations.length; i++) {
                for (Annotation anno : parameterAnnotations[i]) {
                    if (anno instanceof SignatureParam) {
                        // 找到被 @CheckParam 注解修饰的参数
                        signatureParam = (SignatureParam) anno;
                        Object paramValue = args[i];
                        try {
                            paramMap = JSON.parseObject(JSON.toJSONString(paramValue), Map.class);
                        } catch (Exception e) {
                            logger.error("[签名校验] 失败，请检查参数是否正确, paramValue => {}, message => {}", JSON.toJSONString(paramValue), e.getMessage());
                        }
                        break outerLoop;
                    }
                }
            }
            try {
                validateSignature(signatureChecker, signatureParam, paramMap);
            } catch (SignatureValidationException e) {
                logger.warn("[签名校验] 校验失败，paramMap => {}, message => {}", JSON.toJSONString(paramMap), e.getMessage());
                throw e;
            }
        }

        // 继续执行原方法
        return joinPoint.proceed();
    }

    private void validateSignature(SignatureChecker checker, SignatureParam signatureParam, Map<String, Object> paramMap) throws SignatureValidationException {
        // 获取服务编码
        String servicedCodeNew = checker.serviceCode();
        if (StringUtils.isEmpty(servicedCodeNew)) {
            throw new SignatureValidationException("缺失服务编码，请联系管理员配置！");
        }
        // 获取密钥
        String secretKey = StringUtils.isEmpty(checker.secretKey()) ? signatureProperties.getSecretKeys().get(servicedCodeNew) : checker.secretKey();
        if (StringUtils.isEmpty(secretKey)) {
            throw new SignatureValidationException("缺失验签密钥，请联系管理员配置！");
        }

        String signatureField = StringUtils.isEmpty(signatureParam.signatureField()) ? signatureProperties.getSignatureField() : signatureParam.signatureField();
        String requestIdField = StringUtils.isEmpty(signatureParam.requestIdField()) ? signatureProperties.getRequestIdField() : signatureParam.requestIdField();
        String timestampField = StringUtils.isEmpty(signatureParam.timestampField()) ? signatureProperties.getTimestampField() : signatureParam.timestampField();
        // 获取请求参数
        String requestId = paramMap.get(requestIdField) == null ? null : paramMap.get(requestIdField).toString();
        String signature = paramMap.get(signatureField) == null ? null : paramMap.get(signatureField).toString();
        Long timestamp = paramMap.get(timestampField) == null ? null : Long.parseLong(paramMap.get(timestampField).toString());

        if (requestId == null || signature == null || timestamp == null) {
            throw new SignatureValidationException("缺失验签参数，请检查！");
        }

        // 校验时间戳
        validateTimestamp(timestamp, checker.expireMinutes());
        // 校验签名
        if (!SignatureUtil.verifySignature(requestId + timestamp, secretKey, signature)) {
            throw new SignatureValidationException("签名校验不通过！");
        }
    }

    private void validateTimestamp(long timestamp, int expireMinutes) throws SignatureValidationException {
        // 如果是-1，则使用配置文件中的默认值
        expireMinutes = expireMinutes == -1 ? signatureProperties.getExpireMinutes() : expireMinutes;
        // 如果是0，则代表永久有效，不进行时间判断
        if (expireMinutes == 0) {
            return;
        } else if (expireMinutes <= 0) {
            throw new SignatureValidationException("过期时间配置无效，请检查！");
        }

        long currentTime = System.currentTimeMillis();
        if (timestamp > currentTime + 5 * 60 * 1000) {
            throw new SignatureValidationException("调用端时间与服务器时间未同步，请检查！");
        } else if (currentTime - timestamp > (long) expireMinutes * 60 * 1000) {
            throw new SignatureValidationException("请求已过期，请重新请求！");
        }
    }
}
