package com.example.account_service.filter;

import com.example.account_service.kafka.LogProducer;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class LoggingFilter implements Filter {

    private static final String SERVICE_NAME = "account-service";

    // Matches "fieldName": "anyValue" for any field name in the list below,
    // case-insensitive, so the value can be masked before it ever reaches Kafka.
    private static final Pattern SENSITIVE_FIELD_PATTERN = Pattern.compile(
            "(\"(?:password|passwordHash|token|secret)\"\\s*:\\s*)\"[^\"]*\"",
            Pattern.CASE_INSENSITIVE);

    private final LogProducer producer;

    @Override
    public void doFilter(
            ServletRequest request,
            ServletResponse response,
            FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        String path = req.getRequestURI();

        if (!(path.startsWith("/users")
                || path.startsWith("/accounts")
                || path.startsWith("/transactions"))) {

            chain.doFilter(request, response);
            return;
        }

        ContentCachingRequestWrapper requestWrapper =
                new ContentCachingRequestWrapper(req);

        ContentCachingResponseWrapper responseWrapper =
                new ContentCachingResponseWrapper(
                        (jakarta.servlet.http.HttpServletResponse) response);

        chain.doFilter(requestWrapper, responseWrapper);

        String requestBody = new String(
                requestWrapper.getContentAsByteArray(),
                StandardCharsets.UTF_8);

        if (!requestBody.isBlank()) {
            producer.send(
                    SERVICE_NAME,
                    "Request",
                    maskSensitiveFields(requestBody)
            );
        }

        String responseBody = new String(
                responseWrapper.getContentAsByteArray(),
                StandardCharsets.UTF_8);

        if (!responseBody.isBlank()) {
            producer.send(
                    SERVICE_NAME,
                    "Response",
                    maskSensitiveFields(responseBody)
            );
        }

        responseWrapper.copyBodyToResponse();
    }

    /**
     * Replaces the value of any sensitive field (password, passwordHash, token, secret)
     * with a masked placeholder before the body is ever sent to Kafka / persisted to log_dump.
     * The field name and JSON structure are preserved; only the value is redacted.
     */
    private String maskSensitiveFields(String rawBody) {
        if (rawBody == null || rawBody.isBlank()) {
            return rawBody;
        }

        Matcher matcher = SENSITIVE_FIELD_PATTERN.matcher(rawBody);
        return matcher.replaceAll(mr -> Matcher.quoteReplacement(mr.group(1) + "\"***MASKED***\""));
    }
}
