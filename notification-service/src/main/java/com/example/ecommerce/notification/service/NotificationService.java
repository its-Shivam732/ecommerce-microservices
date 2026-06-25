package com.example.ecommerce.notification.service;

import com.example.ecommerce.common.dto.NotificationRequest;
import com.example.ecommerce.notification.entity.Notification;
import com.example.ecommerce.notification.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository repository;

    public NotificationService(NotificationRepository repository) {
        this.repository = repository;
    }

    /**
     * "Send" a notification. In a real system this would dispatch via an email/SMS/push
     * provider; here we log it and persist a record. The persisted log is the deliverable
     * for this assignment.
     */
    @Transactional
    public Notification send(NotificationRequest request) {
        // Simulated dispatch — swap for SES/SNS/push integration in production.
        log.info("Sending [{}] notification to {}: {}",
                request.getChannel(), request.getRecipient(), request.getSubject());

        Notification notification = new Notification(
                request.getRecipient(),
                request.getChannel(),
                request.getSubject(),
                request.getMessage(),
                Notification.Status.SENT,
                request.getRelatedOrderId());

        return repository.save(notification);
    }

    @Transactional(readOnly = true)
    public List<Notification> findAll() {
        return repository.findAll();
    }
}
