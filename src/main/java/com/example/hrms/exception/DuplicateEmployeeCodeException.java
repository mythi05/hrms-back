package com.example.hrms.exception;

public class DuplicateEmployeeCodeException extends RuntimeException {
    public DuplicateEmployeeCodeException(String msg) {
        super(msg);
    }
}
