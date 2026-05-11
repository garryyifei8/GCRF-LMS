package com.gcrf.library.common.security.exception;

import com.gcrf.library.common.exception.BusinessException;
import com.gcrf.library.common.result.ResultCode;

public class AccessDeniedException extends BusinessException {
    public AccessDeniedException(String msg) {
        super(ResultCode.FORBIDDEN.getCode(), msg);
    }
}
