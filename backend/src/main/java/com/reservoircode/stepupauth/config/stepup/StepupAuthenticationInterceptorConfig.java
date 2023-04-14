package com.reservoircode.stepupauth.config.stepup;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Component
public class StepupAuthenticationInterceptorConfig implements WebMvcConfigurer {

    private final StepupAuthenticationInterceptor stepupAuthenticationInterceptor;

    @Autowired
    public StepupAuthenticationInterceptorConfig(
            StepupAuthenticationInterceptor stepupAuthenticationInterceptor
    ) {
        this.stepupAuthenticationInterceptor = stepupAuthenticationInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(stepupAuthenticationInterceptor)
                .order(Ordered.LOWEST_PRECEDENCE);
    }
}