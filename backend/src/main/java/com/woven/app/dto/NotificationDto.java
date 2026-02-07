package com.woven.app.dto;

import com.woven.app.domain.NotificationLog;
import com.woven.app.domain.NotificationType;

import java.time.Instant;

public record NotificationDto(
   Long notificationId,
   Long changeRequestId,
   Integer userId,
   Instant createdTimestamp,
   NotificationType notificationType,
   boolean read
) {
    public static NotificationDto from(NotificationLog notificationLog) {
        return new NotificationDto(
                notificationLog.getNotificationId(),
                notificationLog.getChangeRequest().getChangeRequestId(),
                notificationLog.getUser().getId(),
                notificationLog.getCreatedTimestamp(),
                notificationLog.getNotificationType(),
                notificationLog.getReadTimestamp() != null
        );
    }
}

