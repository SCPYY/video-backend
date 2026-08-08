package com.project.module.social.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.project.common.response.Result;
import com.project.module.social.dto.CommentEditRequest;
import com.project.module.social.dto.CommentRequest;
import com.project.module.social.dto.CommentVO;
import com.project.module.social.dto.LikeResult;
import com.project.module.social.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Tag(name = "评论接口", description = "评论发表、列表查询、点赞/点踩")
@RestController
@RequestMapping("/api/v1/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    /**
     * 获取当前登录用户ID，未登录返回null
     */
    private Long getUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Long) {
            return (Long) auth.getPrincipal();
        }
        return null;
    }

    /**
     * 判断是否为管理员
     */
    private boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getAuthorities() != null) {
            return auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch(r -> r.equals("ROLE_ADMIN") || r.equals("ROLE_EDITOR"));
        }
        return false;
    }

    @Operation(summary = "发表评论/回复")
    @PostMapping
    public Result<CommentVO> publish(@Valid @RequestBody CommentRequest request,
                                     HttpServletRequest httpRequest) {
        String ip = getClientIp(httpRequest);
        return Result.ok(commentService.publishComment(request, getUserId(), ip));
    }

    @Operation(summary = "获取评论列表（含前3条子回复）")
    @GetMapping
    public Result<Page<CommentVO>> list(
            @Parameter(description = "内容ID") @RequestParam Long contentId,
            @Parameter(description = "剧集ID，空=整剧评论") @RequestParam(required = false) Long episodeId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "排序：latest(最新)/hot(最热)") @RequestParam(defaultValue = "latest") String sort) {
        return Result.ok(commentService.getCommentList(contentId, episodeId, getUserId(), page, size, sort));
    }

    @Operation(summary = "获取某条评论的子回复列表")
    @GetMapping("/{id}/replies")
    public Result<Page<CommentVO>> replies(
            @PathVariable Long id,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Integer size) {
        return Result.ok(commentService.getSubReplies(id, getUserId(), page, size));
    }

    @Operation(summary = "编辑评论（仅本人）")
    @PutMapping("/{id}")
    public Result<Void> edit(
            @PathVariable Long id,
            @Valid @RequestBody CommentEditRequest request) {
        commentService.editComment(id, getUserId(), request.getContent());
        return Result.okMsg("修改成功");
    }

    @Operation(summary = "删除评论（本人或管理员）")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        commentService.deleteComment(id, getUserId(), isAdmin());
        return Result.okMsg("已删除");
    }

    @Operation(summary = "点赞/取消点赞")
    @PostMapping("/{id}/like")
    public Result<LikeResult> like(@PathVariable Long id) {
        return Result.ok(commentService.toggleLike(id, getUserId()));
    }

    @Operation(summary = "点踩/取消点踩")
    @PostMapping("/{id}/dislike")
    public Result<LikeResult> dislike(@PathVariable Long id) {
        return Result.ok(commentService.toggleDislike(id, getUserId()));
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多级代理取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
