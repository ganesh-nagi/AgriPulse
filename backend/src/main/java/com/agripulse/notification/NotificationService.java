package com.agripulse.notification;

import com.agripulse.user.User;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** User alerts. Recipients can only read their own notifications. */
@Service
public class NotificationService {

  private final NotificationRepository notifications;

  public NotificationService(NotificationRepository notifications) {
    this.notifications = notifications;
  }

  @Transactional
  public Notification create(User recipient, String title, String message) {
    Notification notification = new Notification();
    notification.setRecipient(recipient);
    notification.setTitle(title);
    notification.setMessage(message);
    return notifications.save(notification);
  }

  @Transactional(readOnly = true)
  public List<Notification> listFor(Long userId) {
    return notifications.findByRecipientIdOrderByCreatedAtDesc(userId);
  }

  @Transactional
  public void markRead(Long notificationId, Long userId) {
    Notification notification =
        notifications
            .findById(notificationId)
            .orElseThrow(() -> new IllegalArgumentException("Notification not found"));
    if (!notification.getRecipient().getId().equals(userId)) {
      throw new AccessDeniedException("Not your notification");
    }
    notification.setRead(true);
  }
}
