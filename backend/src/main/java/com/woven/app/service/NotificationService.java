package com.woven.app.service;

import com.woven.app.domain.NotificationLog;
import com.woven.app.dto.NotificationDto;
import com.woven.app.repository.NotificationLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@Transactional
public class NotificationService {

    private final NotificationLogRepository notificationLogRepository;

    public NotificationService(NotificationLogRepository notificationLogRepository) {
        this.notificationLogRepository = notificationLogRepository;
    }

    public List<NotificationDto> myNotification(Integer userId){
        return notificationLogRepository.findByUser_IdOrderByCreatedTimestamp(userId)
                .stream().map(NotificationDto::from)
                .toList();
    }

    public long unreadCount(Integer userId) {
        return notificationLogRepository.countByUser_IdAndReadTimestampIsNull(userId);
    }

    public void markRead(Long notificationId, Integer userId) {
        NotificationLog notificationLog = notificationLogRepository.findById(notificationId).orElseThrow();

        if (notificationLog.getUser().getId() != userId) {
            throw new SecurityException("You do not have permission to read this notification");
        }

        notificationLog.setReadTimestamp(Instant.now());
        notificationLogRepository.save(notificationLog);

    }

}
