package com.project.backend.exception;


public class DuplicateReturnException extends BusinessException {
    public DuplicateReturnException() {
        super("Return already requested for this item", "DUPLICATE_RETURN");
    }
}