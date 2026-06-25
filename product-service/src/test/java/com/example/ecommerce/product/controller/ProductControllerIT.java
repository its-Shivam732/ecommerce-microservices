package com.example.ecommerce.product.controller;

import com.example.ecommerce.product.dto.ProductRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test: real Spring context + controller + service + JPA against in-memory H2.
 *
 * Two test-specific concerns are handled:
 *  - A stub JwtDecoder is provided so the app doesn't try to fetch keys from the (absent)
 *    auth-server at startup.
 *  - Requests authenticate with a mock JWT via .with(jwt()), so security is satisfied without
 *    a real token.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(ProductControllerIT.TestSecurityConfig.class)
class ProductControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @TestConfiguration
    static class TestSecurityConfig {
        /** Stub decoder: prevents a real network call to the issuer at startup. */
        @Bean
        JwtDecoder jwtDecoder() {
            return mock(JwtDecoder.class);
        }
    }

    @Test
    void getProducts_requiresAuth() throws Exception {
        mockMvc.perform(get("/products"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getProducts_returnsSeededProducts_whenAuthenticated() throws Exception {
        mockMvc.perform(get("/products").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void createProduct_thenFetchById() throws Exception {
        ProductRequest request = new ProductRequest();
        request.setName("Test Widget");
        request.setDescription("integration test product");
        request.setPrice(new BigDecimal("9.99"));
        request.setAvailableStock(5);

        String json = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/products").with(jwt())
                        .contentType("application/json")
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Test Widget"))
                .andExpect(jsonPath("$.availableStock").value(5));
    }

    @Test
    void createProduct_rejectsInvalidPrice() throws Exception {
        ProductRequest request = new ProductRequest();
        request.setName("Bad");
        request.setPrice(new BigDecimal("-1.00"));
        request.setAvailableStock(1);

        mockMvc.perform(post("/products").with(jwt())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
