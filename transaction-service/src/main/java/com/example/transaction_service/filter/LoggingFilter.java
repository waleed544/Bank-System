package com.example.transaction_service.filter;

import com.example.transaction_service.kafka.LogProducer;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class LoggingFilter implements Filter {

    private final LogProducer producer;

    @Override
    public void doFilter(
            ServletRequest request,
            ServletResponse response,
            FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
//        String path = req.getRequestURI();
//
//        if (path.startsWith("/swagger-ui")
//                || path.contains(".css")
//                || path.contains(".png")
//                || path.contains(".svg")
//                || path.contains(".ico")
//                || path.startsWith("/v3/api-docs")
//                || path.startsWith("/webjars")
//                || path.startsWith("/favicon.ico")) {
//
//            chain.doFilter(request, response);
//            return;
//        }
        String path = req.getRequestURI();

        if (!(path.startsWith("/users")
                || path.startsWith("/accounts")
                || path.startsWith("/transactions"))) {

            chain.doFilter(request, response);
            return;
        }



        ContentCachingRequestWrapper requestWrapper =
                new ContentCachingRequestWrapper((HttpServletRequest) request);

        ContentCachingResponseWrapper responseWrapper =
                new ContentCachingResponseWrapper(
                        (jakarta.servlet.http.HttpServletResponse) response);

        chain.doFilter(requestWrapper, responseWrapper);

        String requestBody = new String(
                requestWrapper.getContentAsByteArray(),
                StandardCharsets.UTF_8);

        if (!requestBody.isBlank()) {
            producer.send(
                    "transaction-service",
                    "Request",
                    requestBody
            );
        }

        String responseBody = new String(
                responseWrapper.getContentAsByteArray(),
                StandardCharsets.UTF_8);

        if (!responseBody.isBlank()) {
            producer.send(
                    "transaction-service",
                    "Response",
                    responseBody
            );
        }

        responseWrapper.copyBodyToResponse();
    }
}