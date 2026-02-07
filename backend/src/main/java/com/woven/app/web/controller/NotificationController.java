package com.woven.app.web.controller;

import com.woven.app.service.NotificationService;
import com.woven.app.service.user.AppUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.woven.app.dto.NotificationDto;

import java.util.List;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/mine")
    public List<NotificationDto>  myNotifications(@AuthenticationPrincipal AppUserDetails user) {
        return notificationService.myNotification(user.getUser().getId());
    }

    @GetMapping("/mine/unread-count")
    public long unreadCount(@AuthenticationPrincipal AppUserDetails user) {
        return notificationService.unreadCount(user.getUser().getId());
    }

    @PostMapping("/{notificationId}/read")
    public void markAsRead(@PathVariable Long notificationId, @AuthenticationPrincipal AppUserDetails user) {
        notificationService.markRead(notificationId, user.getUser().getId());
    }

}
