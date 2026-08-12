package com.project.module.message.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.project.common.response.Result;
import com.project.module.message.entity.SystemMessage;
import com.project.module.message.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Tag(name="消息中心", description="用户和管理员站内消息")
@RestController
@RequestMapping("/api/v1/messages")
@RequiredArgsConstructor
public class MessageController {
    private final MessageService service;
    private Long id() { return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal(); }
    private String type() { return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")) ? "ADMIN" : "USER"; }
    @Operation(summary="消息列表") @GetMapping
    public Result<Page<SystemMessage>> list(@RequestParam(defaultValue="1") Integer page, @RequestParam(defaultValue="20") Integer size) { return Result.ok(service.page(type(), id(), page, size)); }
    @Operation(summary="未读数量") @GetMapping("/unread-count")
    public Result<Long> unread() { return Result.ok(service.unread(type(), id())); }
    @Operation(summary="标记已读") @PutMapping("/{messageId}/read")
    public Result<Void> read(@PathVariable Long messageId) { service.read(id(), type(), messageId); return Result.okMsg("已读"); }
    @Operation(summary="全部已读") @PutMapping("/read-all")
    public Result<Void> readAll() { service.readAll(id(), type()); return Result.okMsg("已全部读"); }
}
