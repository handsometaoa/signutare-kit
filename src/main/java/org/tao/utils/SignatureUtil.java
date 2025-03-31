package org.tao.utils;

import org.springframework.util.DigestUtils;

/**
 * @author: handsometaoa
 * @description
 * @date: 2025/3/29 11:00
 */
public class SignatureUtil {

    /**
     * 校验签名是否正确
     *
     * @param params    请求参数
     * @param sign      客户端传递的签名
     * @param secretKey 密钥
     * @return 是否校验通过
     */
    public static boolean verifySignature(String params, String secretKey, String sign) {
        String serverSign = generateSignature(params, secretKey);
        return serverSign.equals(sign);
    }


    /**
     * 生成签名
     *
     * @param requestId 请求id
     * @param timestamp 时间戳
     * @param secretKey 密钥
     * @return 生成的签名
     */
    public static String generateSignature(String requestId, String timestamp, String secretKey) {
        String rawData = requestId + timestamp + secretKey;
        return DigestUtils.md5DigestAsHex(rawData.getBytes());
    }



    private static String generateSignature(String params, String secretKey) {
        String rawData = params + secretKey;
        return DigestUtils.md5DigestAsHex(rawData.getBytes());
    }


}

