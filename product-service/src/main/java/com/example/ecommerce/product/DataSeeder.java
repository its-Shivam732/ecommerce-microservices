package com.example.ecommerce.product;

import com.example.ecommerce.product.entity.Product;
import com.example.ecommerce.product.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

/** Seeds sample products on startup (H2 is in-memory, so it starts empty each run). */
@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner seedProducts(ProductRepository repository) {
        return args -> {
            if (repository.count() > 0) {
                return;
            }
            repository.save(new Product("Wireless Mouse",
                    "Ergonomic 2.4GHz wireless mouse", new BigDecimal("24.99"), 100));
            repository.save(new Product("Mechanical Keyboard",
                    "RGB backlit, brown switches", new BigDecimal("79.99"), 50));
            repository.save(new Product("USB-C Hub",
                    "7-in-1 multiport adapter", new BigDecimal("39.99"), 75));
            repository.save(new Product("1080p Webcam",
                    "Full HD webcam with privacy shutter", new BigDecimal("49.99"), 30));
        };
    }
}
