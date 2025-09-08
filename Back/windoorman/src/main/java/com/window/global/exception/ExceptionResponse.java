package com.window.global.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ExceptionResponse extends RuntimeException {

    private ErrorCode customException;
}
