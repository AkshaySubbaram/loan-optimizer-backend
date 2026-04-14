package com.loan.closure.exception;

public class LoanCompletedException extends RuntimeException {
    public LoanCompletedException(String message) {
        super(message);
    }

    public LoanCompletedException(String message, Throwable cause) {
        super(message, cause);
    }
}

