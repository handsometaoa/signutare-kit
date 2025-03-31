package org.tao.exception;

import org.tao.consts.SignatureConst;

public class SignatureValidationException extends RuntimeException {

    private String returnType = SignatureConst.DEFAULT_RETURN_TYPE;

    public SignatureValidationException(String returnType, String message) {
        super(message);
        this.returnType = returnType;
    }

    public String getReturnType() {
        return returnType;
    }
}
