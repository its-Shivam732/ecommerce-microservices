package com.example.ecommerce.notification.repository;

import com.example.ecommerce.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
}
