package com.example.ecommerce.notification.controller;

import com.example.ecommerce.common.dto.NotificationRequest;
import com.example.ecommerce.notification.dto.NotificationResponse;
import com.example.ecommerce.notification.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping
    public ResponseEntity<NotificationResponse> send(
            @Valid @RequestBody NotificationRequest request) {
        NotificationResponse response =
                NotificationResponse.from(notificationService.send(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public List<NotificationResponse> getAll() {
        return notificationService.findAll().stream().map(NotificationResponse::from).toList();
    }
}
