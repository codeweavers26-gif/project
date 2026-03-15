package com.project.backend.exception;


public class ReturnWindowExpiredException extends BusinessException {
    public ReturnWindowExpiredException() {
        super("Return window has expired (30 days from purchase)", "RETURN_WINDOW_EXPIRED");
    }
}


