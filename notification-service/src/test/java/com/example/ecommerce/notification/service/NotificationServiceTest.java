package com.example.ecommerce.notification.service;

import com.example.ecommerce.common.dto.NotificationRequest;
import com.example.ecommerce.notification.entity.Notification;
import com.example.ecommerce.notification.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository repository;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void send_persistsNotificationWithSentStatus() {
        when(repository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        NotificationRequest request = new NotificationRequest(
                "ada@example.com", "EMAIL", "Hi", "Your order shipped", 5L);

        Notification result = notificationService.send(request);

        assertThat(result.getRecipient()).isEqualTo("ada@example.com");
        assertThat(result.getChannel()).isEqualTo("EMAIL");
        assertThat(result.getStatus()).isEqualTo(Notification.Status.SENT);
        assertThat(result.getRelatedOrderId()).isEqualTo(5L);
        verify(repository).save(any(Notification.class));
    }
}
