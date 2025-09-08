package com.window.global.security.handler;

import com.window.global.exception.CustomException;
import com.window.global.exception.ErrorCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
public class CustomExceptionHandler implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        ErrorCode exception = (ErrorCode) request.getAttribute("exception");
        if(exception == null){
            exception = ErrorCode.ACCESS_DENIED_EXCEPTION;
        }

        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().println("{ \"message\" : \"" + exception.getErrorMessage()
                + "\", \"code\" : \"" +  exception.getErrorCode()
                + "\", \"statusNum\" : " + exception.getStatusNum()
                + "}");
    }
}
