package com.reservoircode.stepupauth.config.stepup;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Log4j2
@Component
public class StepupAuthenticationInterceptor implements HandlerInterceptor {

    private static final String OTP_SERVER_URI = "%s/protocol/openid-connect/auth?client_id=%s&redirect_uri=%s&response_type=code&response_mode=query&scope=openid&acr_values=%s";

    private static final String ACR_CLAIM_NAME = "acr";
    private static final String ACR_CLAIM_LEVEL = "2";
    private static final String ORIGIN_URL_HEADER = "referer";
    private static final String CALLBACK_URL_HEADER = "x-api-cb";

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;
    @Value("${spring.security.oauth2.resourceserver.jwt.client-id}")
    private String clientId;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public record ResponseDto(HttpStatus status, String authenticationUrl) {
    }

    @Override
    public boolean preHandle(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler
    ) throws IOException {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        if (!handlerMethod.hasMethodAnnotation(StepupAuthentication.class)) {
            return true;
        }
        Optional<Jwt> authorizationTokenOpt = getAuthorizationToken();

        if (authorizationTokenOpt.isEmpty()) {
            log.warn("Invalid Authorization token found when querying: {}", request.getRequestURI());
            response.setStatus(HttpStatus.FORBIDDEN.value());
            return false;
        }
        Optional<String> acrClaimValueOpt = getAcrClaim(authorizationTokenOpt.get());

        if (acrClaimValueOpt.isEmpty()) {
            log.warn("ACR claim invalid or not found when querying: {}", request.getRequestURI());
            response.setStatus(HttpStatus.FORBIDDEN.value());
            return false;
        }
        if (ACR_CLAIM_LEVEL.equals(acrClaimValueOpt.get())) {
            log.info("Calling resource with step-up authentication level for: {}", request.getRequestURI());
            return true;
        }
        log.info("Need to step-up authentication level for: {}", request.getRequestURI());
        String steppedUpAuthenticationUrl = buildSteppedUpAuthenticationUrl(request, issuerUri, clientId);
        ResponseDto responseDto = new ResponseDto(HttpStatus.PRECONDITION_FAILED, steppedUpAuthenticationUrl);

        response.setStatus(responseDto.status().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getOutputStream().write(objectMapper.writeValueAsString(responseDto).getBytes());

        return false;
    }

    private static Optional<Jwt> getAuthorizationToken() {
        return Optional.ofNullable(SecurityContextHolder.getContext())
                .map(SecurityContext::getAuthentication)
                .map(Authentication::getPrincipal)
                .filter(Jwt.class::isInstance)
                .map(Jwt.class::cast);
    }

    private static Optional<String> getAcrClaim(Jwt authorizationToken) {
        return Optional.ofNullable(
                authorizationToken.getClaim(ACR_CLAIM_NAME));
    }

    private static String buildSteppedUpAuthenticationUrl(
            HttpServletRequest request,
            String issuerUri,
            String clientId
    ) {
        String referer = Optional.ofNullable(request.getHeader(ORIGIN_URL_HEADER)).orElse("/");
        String callback = Optional.ofNullable(request.getHeader(CALLBACK_URL_HEADER)).orElse("/");
        String redirectUri = URLEncoder.encode(referer + callback, StandardCharsets.UTF_8);

        return OTP_SERVER_URI.formatted(issuerUri, clientId, redirectUri, ACR_CLAIM_LEVEL);
    }
}