package com.project.module.message.service;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.project.module.message.entity.SystemMessage;
public interface MessageService { void send(String recipientType, Long recipientId, String type, String actionType, String title, String content, String targetType, Long targetId, String targetUrl, String relatedType, Long relatedId); Page<SystemMessage> page(String recipientType, Long recipientId, Integer page, Integer size); long unread(String recipientType, Long recipientId); void read(Long recipientId, String recipientType, Long messageId); void readAll(Long recipientId, String recipientType); }
