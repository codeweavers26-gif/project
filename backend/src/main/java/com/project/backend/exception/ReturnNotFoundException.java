package com.project.backend.exception;

public class ReturnNotFoundException extends BusinessException {
    public ReturnNotFoundException(Long id) {
        super("Return not found with ID: " + id, "RETURN_NOT_FOUND");
    }
}





