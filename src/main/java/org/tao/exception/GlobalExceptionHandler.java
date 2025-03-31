package org.tao.exception;

import com.alibaba.fastjson2.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.tao.config.SignatureProperties;
import org.tao.consts.SignatureConst;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    @Resource
    private SignatureProperties signatureProperties;

    @ExceptionHandler(SignatureValidationException.class)
    public ResponseEntity<Map<String, Object>> handleSignatureValidationException(SignatureValidationException ex) {
        Map<String, Object> map = new HashMap<>();
        map.put("code", 500);
        map.put("message", ex.getMessage());
        if (ex.getReturnType() != null && !ex.getReturnType().equals(SignatureConst.DEFAULT_RETURN_TYPE)) {
            String responseStr = signatureProperties.getReturnJsons().get(ex.getReturnType());
            if (StringUtils.isEmpty(responseStr)) {
                log.error("[全局异常处理] 返回json配置错误，请检查配置！");
            } else {
                responseStr = responseStr.replace("${message}", ex.getMessage());
            }
            map = JSON.parseObject(responseStr, Map.class);
        }
        return new ResponseEntity<>(map, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
