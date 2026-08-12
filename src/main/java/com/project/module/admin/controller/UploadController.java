package com.project.module.admin.controller;

import com.project.common.response.Result;
import com.project.module.admin.dto.ImageUploadVO;
import com.project.module.admin.dto.VideoUploadVO;
import com.project.module.admin.service.UploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "管理后台-文件上传", description = "图片、视频上传到本地服务器")
@RestController
@RequestMapping("/api/v1/admin/upload")
@RequiredArgsConstructor
public class UploadController {

    private final UploadService uploadService;

    /**
     * 获取当前管理员ID
     */
    private Long getAdminId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Long) {
            return (Long) auth.getPrincipal();
        }
        return null;
    }

    @Operation(summary = "上传图片", description = "保存到本地 uploads/images 目录，返回可访问 URL。支持 jpg/jpeg/png/webp/gif，最大 10MB。")
    @PostMapping("/image")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public Result<ImageUploadVO> uploadImage(
            @Parameter(description = "图片文件，支持jpg/png/webp/gif，最大10MB")
            @RequestParam("file") MultipartFile file) {
        return Result.ok(uploadService.uploadImage(file, getAdminId()));
    }

    @Operation(summary = "上传视频", description = "保存到本地 uploads/videos 目录，并使用 FFprobe 识别视频时长，返回秒数。支持 mp4/mkv/mov/webm/avi，最大 2GB。")
    @PostMapping("/video")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public Result<VideoUploadVO> uploadVideo(
            @Parameter(description = "视频文件，支持mp4/mkv/mov，最大2GB")
            @RequestParam("file") MultipartFile file) {
        return Result.ok(uploadService.uploadVideo(file, getAdminId()));
    }
}
