package org.tao.enums;

public enum SignatureParamTypeEnum {

    SERVICE_CODE(1, "服务编码"),
    PARAMS(2, "参数");

    private Integer code;
    private String desc;

    SignatureParamTypeEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() {
        return code;
    }


}