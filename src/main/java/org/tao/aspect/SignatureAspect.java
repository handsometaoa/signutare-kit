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
import org.tao.consts.SignatureConst;
import org.tao.enums.SignatureParamTypeEnum;
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

    @Around("@annotation(org.tao.anno.SignatureChecker) " +
            "&& (@annotation(org.springframework.web.bind.annotation.PostMapping) || @annotation(org.springframework.web.bind.annotation.RequestMapping))")
    public Object validateSignature(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        Map<String, Object> paramMap = null;
        String serviceCode = null;
        Object[] args = joinPoint.getArgs();
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        SignatureChecker signatureChecker = method.getAnnotation(SignatureChecker.class);
        if (signatureChecker != null && signatureChecker.required()) {
            SignatureParam signatureParam = null;
            // 遍历参数注解，找到被 @SignatureParam 注解修饰的参数
            for (int i = 0; i < parameterAnnotations.length; i++) {
                for (Annotation anno : parameterAnnotations[i]) {
                    if (anno instanceof SignatureParam) {
                        signatureParam = (SignatureParam) anno;
                        Object paramValue = args[i];
                        if (signatureParam.type() == SignatureParamTypeEnum.PARAMS) {
                            try {
                                paramMap = JSON.parseObject(JSON.toJSONString(paramValue), Map.class);
                            } catch (Exception e) {
                                logger.error("[签名校验] 失败，请检查 params 参数是否正确, paramsValue => {}, message => {}", JSON.toJSONString(paramValue), e.getMessage());
                            }
                        } else if (signatureParam.type() == SignatureParamTypeEnum.SERVICE_CODE) {
                            if (paramValue instanceof String) {
                                serviceCode = paramValue.toString();
                            } else {
                                logger.error("[签名校验] 失败，请检查 serviceCode 参数是否正确, serviceCode => {}", JSON.toJSONString(paramValue));
                            }
                        } else {
                            throw new SignatureValidationException(SignatureConst.DEFAULT_RETURN_TYPE, "参数类型错误，请检查！");
                        }
                    }
                }
            }

            try {
                validateSignature(signatureChecker, signatureParam, paramMap, serviceCode);
            } catch (SignatureValidationException e) {
                logger.warn("[签名校验] 校验失败，paramMap => {}, message => {}", JSON.toJSONString(paramMap), e.getMessage());
                throw e;
            } catch (Exception e){
                logger.error("[签名校验] 校验失败，paramMap => {}, message => {}", JSON.toJSONString(paramMap), e.getMessage());
                throw new SignatureValidationException(SignatureConst.DEFAULT_RETURN_TYPE, "系统异常，请稍后重试！");
            }
        }

        // 继续执行原方法
        return joinPoint.proceed();
    }

    private void validateSignature(SignatureChecker checker, SignatureParam signatureParam, Map<String, Object> paramMap, String serviceCode) throws SignatureValidationException {
        // 获取服务编码，如果 SignatureChecker 指定，选择 SignatureChecker 的值，否则选择 signatureParam 标记的值
        String servicedCodeNew = StringUtils.isEmpty(checker.serviceCode()) ? serviceCode : checker.serviceCode();
        if (StringUtils.isEmpty(servicedCodeNew)) {
            throw new SignatureValidationException(checker.returnType(), "[验签失败] 缺失 serviceCode，请配置！");
        }

        // 获取密钥 如果 SignatureChecker 指定，选择 SignatureChecker 的值，否则选择 signatureProperties 配置的值
        String secretKey = StringUtils.isEmpty(checker.secretKey()) ? signatureProperties.getSecretKeys().get(servicedCodeNew) : checker.secretKey();
        if (StringUtils.isEmpty(secretKey)) {
            throw new SignatureValidationException(checker.returnType(), "[验签失败] 缺失 secretKey，请配置！");
        }

        // 获取参数字段 如果 signatureParam 指定，选择 signatureParam 的值，否则选择 signatureProperties 配置的值
        String signatureField = StringUtils.isEmpty(signatureParam.signatureField()) ? signatureProperties.getSignatureField() : signatureParam.signatureField();
        String requestIdField = StringUtils.isEmpty(signatureParam.requestIdField()) ? signatureProperties.getRequestIdField() : signatureParam.requestIdField();
        String timestampField = StringUtils.isEmpty(signatureParam.timestampField()) ? signatureProperties.getTimestampField() : signatureParam.timestampField();

        // 获取实际请求参数数据
        String requestId = paramMap.get(requestIdField) == null ? null : paramMap.get(requestIdField).toString();
        Long timestamp = paramMap.get(timestampField) == null ? null : Long.parseLong(paramMap.get(timestampField).toString());
        String signature = paramMap.get(signatureField) == null ? null : paramMap.get(signatureField).toString();
        if (StringUtils.isEmpty(requestId) || StringUtils.isEmpty(signature) || StringUtils.isEmpty(timestamp)) {
            logger.warn("[验签失败] 缺失鉴权参数，请检查！requestId => {}, signature => {}, timestamp => {}", requestId, signature, timestamp);
            throw new SignatureValidationException(checker.returnType(), "[验签失败] 缺失鉴权参数，请检查！");
        }

        // 校验时间戳
        validateTimestamp(checker, timestamp);

        // 校验签名
        if (!SignatureUtil.verifySignature(requestId + timestamp, secretKey, signature)) {
            throw new SignatureValidationException(checker.returnType(), "签名校验不通过！");
        }
    }

    private void validateTimestamp(SignatureChecker checker, long timestamp) throws SignatureValidationException {
        // 如果是-1，则使用配置文件中的默认值
        long expireMinutes = checker.expireMinutes() == -1 ? signatureProperties.getExpireMinutes() : checker.expireMinutes();
        // 如果是0，则代表永久有效，不进行时间判断
        if (expireMinutes == 0) {
            return;
        } else if (expireMinutes <= 0) {
            throw new SignatureValidationException(checker.returnType(), "[验签失败] 过期时间配置无效，请检查！");
        }

        long currentTime = System.currentTimeMillis();
        if (timestamp > currentTime + 5 * 60 * 1000) {
            throw new SignatureValidationException(checker.returnType(), "[验签失败] 调用端时间与服务器时间未同步，请检查！");
        } else if (currentTime - timestamp > (long) expireMinutes * 60 * 1000) {
            throw new SignatureValidationException(checker.returnType(), "[验签失败] 请求已过期，请重新请求！");
        }
    }
}
