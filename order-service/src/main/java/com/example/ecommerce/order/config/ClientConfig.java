package com.example.ecommerce.order.config;

import com.example.ecommerce.order.client.ServiceTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * HTTP clients for downstream services. Two concerns are configured here:
 *
 *  1. Short timeouts so a hung downstream surfaces as a fast failure the circuit breaker can
 *     act on, rather than tying up order threads.
 *  2. A bearer-token interceptor: product-service and notification-service are now protected
 *     resource servers, so every outbound call must carry a client-credentials access token
 *     obtained from the auth-server.
 */
@Configuration
public class ClientConfig {

    private final ServiceTokenProvider tokenProvider;

    public ClientConfig(ServiceTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    private static SimpleClientHttpRequestFactory timeouts() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(2).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(2).toMillis());
        return factory;
    }

    /** Adds "Authorization: Bearer <token>" to every outbound request. */
    private ClientHttpRequestInterceptor bearerTokenInterceptor() {
        return (request, body, execution) -> {
            request.getHeaders().setBearerAuth(tokenProvider.getAccessToken());
            return execution.execute(request, body);
        };
    }

    @Bean
    RestClient productServiceClient(
            RestClient.Builder builder,
            @Value("${clients.product-service.base-url}") String baseUrl) {
        return builder
                .baseUrl(baseUrl)
                .requestFactory(timeouts())
                .requestInterceptor(bearerTokenInterceptor())
                .build();
    }

    @Bean
    RestClient notificationServiceClient(
            RestClient.Builder builder,
            @Value("${clients.notification-service.base-url}") String baseUrl) {
        return builder
                .baseUrl(baseUrl)
                .requestFactory(timeouts())
                .requestInterceptor(bearerTokenInterceptor())
                .build();
    }
}
