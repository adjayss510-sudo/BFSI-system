package com.bfsi.auth;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

public class InternalFeignConfig {
    @Bean
    RequestInterceptor internalKey(@Value("${internal.secret}") String secret) {
        return t -> t.header("X-Internal-Secret", secret);
    }
}
