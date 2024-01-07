package com.bank.gatewayserver.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

import java.time.Duration;
import java.time.LocalDateTime;

@Configuration
public class RouteLocatorConfig {
    @Bean
    public RouteLocator bankRoutes(RouteLocatorBuilder builder, RedisRateLimiter redisRateLimiter, KeyResolver userKeyResolver) {
        return builder.routes()
                .route(p -> p
                        .path("/bank/accounts/**")
                        .filters(f -> f
                                .rewritePath("/bank/accounts/(?<segment>.*)", "/${segment}")
                                .removeRequestHeader("Cookie")
                                .addResponseHeader("X-Response-Time", LocalDateTime.now().toString())
                                // Below line will need resilience4j library in pom.xml
                                .circuitBreaker(config -> config.setName("accounts-circuit-breaker").setFallbackUri("forward:/contactSupport"))
                        )
                        //.uri("lb://bank-accounts")) // when EUREKA is used
                        .uri("http://bank-accounts:8080")) // when KUBERNETES SERVICE DISCOVERY is used
                .route(p -> p
                        .path("/bank/cards/**")
                        .filters(f -> f
                                .rewritePath("/bank/cards/(?<segment>.*)", "/${segment}")
                                .removeRequestHeader("Cookie")
                                // GET is Idempotent
                                .retry(retryConfig -> retryConfig.setRetries(3)//.setExceptions()
                                        .setMethods(HttpMethod.GET)
                                        // gateway initially waits for 100ms, it will then increment backoff duration capping it at max 1000ms
                                        .setBackoff(Duration.ofMillis(100), Duration.ofMillis(1000), 2, true))
                        )
                        //.uri("lb://bank-accounts")) // when EUREKA is used
                        .uri("http://bank-cards:9000")) // when KUBERNETES SERVICE DISCOVERY is used
                .route(p -> p
                        .path("/bank/loans/**")
                        .filters(f -> f
                                .rewritePath("/bank/loans/(?<segment>.*)", "/${segment}")
                                .removeRequestHeader("Cookie")
                                .requestRateLimiter(config -> config.setRateLimiter(redisRateLimiter)
                                        .setKeyResolver(userKeyResolver)
                                )
                        )
                        //.uri("lb://bank-accounts")) // when EUREKA is used
                        .uri("http://bank-loans:8090")) // when KUBERNETES SERVICE DISCOVERY is used

                .build();
    }
}
