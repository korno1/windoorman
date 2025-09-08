package com.window.global.exception;

import com.window.global.util.HttpResponseUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final HttpResponseUtil responseUtil;

    @ExceptionHandler(ExceptionResponse.class)
    public ResponseEntity<?> handlerException(ExceptionResponse e) {
        Map<String, Object> errorMap = new HashMap<>();
        errorMap.put("message", e.getCustomException().getErrorMessage());
        errorMap.put("code", e.getCustomException().getErrorCode());
        errorMap.put("status", e.getCustomException().getStatusNum());
        return responseUtil.errorResponse(HttpStatus.valueOf(e.getCustomException().getStatusNum()), errorMap);
    }
}
