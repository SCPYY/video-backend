package com.project.module.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.project.common.response.Result;
import com.project.module.admin.service.AdminLogService;
import com.project.module.social.entity.Comment;
import com.project.module.social.entity.CommentReport;
import com.project.module.social.entity.Danmaku;
import com.project.module.social.mapper.CommentMapper;
import com.project.module.social.mapper.CommentReportMapper;
import com.project.module.social.mapper.DanmakuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "管理后台-社区治理", description = "评论、弹幕和举报内容审核")
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminSocialController {
    private final CommentMapper commentMapper;
    private final CommentReportMapper reportMapper;
    private final DanmakuMapper danmakuMapper;
    private final AdminLogService adminLogService;

    @GetMapping("/comments")
    @Operation(summary = "评论分页列表", description = "按内容 ID 和评论状态查询评论。状态：1-正常，2-已删除，3-审核中。")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR','VIEWER')")
    public Result<Page<Comment>> comments(@RequestParam(required = false) Long contentId,
                                          @RequestParam(required = false) Integer status,
                                          @RequestParam(defaultValue = "1") Integer page,
                                          @RequestParam(defaultValue = "20") Integer size) {
        return Result.ok(commentMapper.selectPage(new Page<>(safePage(page), safeSize(size)),
                new LambdaQueryWrapper<Comment>().eq(contentId != null, Comment::getContentId, contentId)
                        .eq(status != null, Comment::getStatus, status).orderByDesc(Comment::getId)));
    }

    @PutMapping("/comments/{id}/status")
    @Operation(summary = "更新评论状态", description = "更新评论审核状态：1-正常，2-已删除，3-审核中。")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public Result<Void> commentStatus(@PathVariable Long id, @RequestParam Integer status) {
        Comment comment = commentMapper.selectById(id);
        if (comment != null) {
            Integer before = comment.getStatus(); comment.setStatus(status); commentMapper.updateById(comment);
            adminLogService.log(adminId(), "STATUS", "COMMENT", String.valueOf(id), before, status);
        }
        return Result.okMsg("评论状态已更新");
    }

    @GetMapping("/danmaku")
    @Operation(summary = "弹幕分页列表", description = "按剧集 ID 和弹幕状态查询弹幕。状态：1-正常，2-已删除。")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR','VIEWER')")
    public Result<Page<Danmaku>> danmaku(@RequestParam(required = false) Long episodeId,
                                         @RequestParam(required = false) Integer status,
                                         @RequestParam(defaultValue = "1") Integer page,
                                         @RequestParam(defaultValue = "20") Integer size) {
        return Result.ok(danmakuMapper.selectPage(new Page<>(safePage(page), safeSize(size)),
                new LambdaQueryWrapper<Danmaku>().eq(episodeId != null, Danmaku::getEpisodeId, episodeId)
                        .eq(status != null, Danmaku::getStatus, status).orderByDesc(Danmaku::getId)));
    }

    @PutMapping("/danmaku/{id}/status")
    @Operation(summary = "更新弹幕状态", description = "更新弹幕状态：1-正常，2-已删除。")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public Result<Void> danmakuStatus(@PathVariable Long id, @RequestParam Integer status) {
        Danmaku danmaku = danmakuMapper.selectById(id);
        if (danmaku != null) {
            Integer before = danmaku.getStatus(); danmaku.setStatus(status); danmakuMapper.updateById(danmaku);
            adminLogService.log(adminId(), "STATUS", "DANMAKU", String.valueOf(id), before, status);
        }
        return Result.okMsg("弹幕状态已更新");
    }

    @GetMapping("/comment-reports")
    @Operation(summary = "举报分页列表", description = "按举报处理状态查询评论举报。状态：0-待处理，1-已处理，2-驳回。")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR','VIEWER')")
    public Result<Page<CommentReport>> reports(@RequestParam(required = false) Integer status,
                                               @RequestParam(defaultValue = "1") Integer page,
                                               @RequestParam(defaultValue = "20") Integer size) {
        return Result.ok(reportMapper.selectPage(new Page<>(safePage(page), safeSize(size)),
                new LambdaQueryWrapper<CommentReport>().eq(status != null, CommentReport::getStatus, status)
                        .orderByDesc(CommentReport::getId)));
    }

    @PutMapping("/comment-reports/{id}/status")
    @Operation(summary = "更新举报状态", description = "更新举报处理状态：0-待处理，1-已处理，2-驳回。")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public Result<Void> reportStatus(@PathVariable Long id, @RequestParam Integer status) {
        CommentReport report = reportMapper.selectById(id);
        if (report != null) {
            Integer before = report.getStatus(); report.setStatus(status); reportMapper.updateById(report);
            adminLogService.log(adminId(), "STATUS", "COMMENT_REPORT", String.valueOf(id), before, status);
        }
        return Result.okMsg("举报状态已更新");
    }

    private Long adminId() { return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal(); }
    private int safePage(Integer page) { return page == null || page < 1 ? 1 : page; }
    private int safeSize(Integer size) { return size == null ? 20 : Math.min(100, Math.max(1, size)); }
}
