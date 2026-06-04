package com.banking.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RouteConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()

                // Auth Service Routes
                .route("auth-service", r -> r.path("/api/auth/**")
                        .uri("lb://auth-service"))

                // User Service Routes
                .route("user-service", r -> r.path("/api/user/profile/**", "/api/admin/users/**")
                        .uri("lb://user-service"))

                // Account Service Routes
                .route("account-service", r -> r.path("/api/user/accounts/**", "/api/admin/freeze-account/**",
                                "/api/admin/unfreeze-account/**", "/api/upi/resolve/**")
                        .uri("lb://account-service"))

                // Transaction Service Routes
                .route("transaction-service", r -> r.path("/api/user/transfer/**", "/api/user/history/**",
                                "/api/admin/transactions/**", "/api/user/refund/**")
                        .uri("lb://transaction-service"))

                // Ledger Service Routes
                .route("ledger-service", r -> r.path("/api/ledger/**", "/api/admin/ledger/**")
                        .uri("lb://ledger-service"))

                // UPI Service Routes
                .route("upi-service", r -> r.path("/api/user/upi/**", "/api/user/upi-pay/**")
                        .uri("lb://upi-service"))

                // Currency Service Routes
                .route("currency-service", r -> r.path("/api/currency/**")
                        .uri("lb://currency-service"))

                // Fraud Service Routes
                .route("fraud-service", r -> r.path("/api/admin/fraud-alerts/**")
                        .uri("lb://fraud-service"))

                // Notification Service Routes
                .route("notification-service", r -> r.path("/api/user/notifications/**", "/ws/notifications/**")
                        .uri("lb://notification-service"))

                // Audit Service Routes
                .route("audit-service", r -> r.path("/api/admin/audit-logs/**")
                        .uri("lb://audit-service"))

                // Admin Monitoring Service Routes
                .route("admin-monitoring-service", r -> r.path("/api/admin/analytics/**",
                                "/api/admin/health/**", "/api/admin/settlements/**", "/ws/admin/**")
                        .uri("lb://admin-monitoring-service"))

                .build();
    }
}
