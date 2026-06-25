package com.example.ecommerce.order.client;

import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.stereotype.Component;

/**
 * Obtains a client-credentials access token for service-to-service calls. The underlying
 * manager caches the token and only contacts the auth-server again when it expires.
 */
@Component
public class ServiceTokenProvider {

    private static final String CLIENT_REGISTRATION_ID = "ecommerce-client";

    private final OAuth2AuthorizedClientManager authorizedClientManager;

    public ServiceTokenProvider(OAuth2AuthorizedClientManager authorizedClientManager) {
        this.authorizedClientManager = authorizedClientManager;
    }

    /** Returns a bearer token value, or throws if the auth-server can't be reached. */
    public String getAccessToken() {
        OAuth2AuthorizeRequest request = OAuth2AuthorizeRequest
                .withClientRegistrationId(CLIENT_REGISTRATION_ID)
                .principal("order-service")
                .build();

        OAuth2AuthorizedClient client = authorizedClientManager.authorize(request);
        if (client == null || client.getAccessToken() == null) {
            throw new IllegalStateException("Unable to obtain access token for outbound call");
        }
        return client.getAccessToken().getTokenValue();
    }
}
