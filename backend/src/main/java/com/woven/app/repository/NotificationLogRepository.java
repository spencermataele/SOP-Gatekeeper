package com.woven.app.repository;

import com.woven.app.domain.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {

    List<NotificationLog> findByUser_IdOrderByCreatedTimestamp(Integer userId);

    long countByUser_IdAndReadTimestampIsNull(Integer userId);
}
