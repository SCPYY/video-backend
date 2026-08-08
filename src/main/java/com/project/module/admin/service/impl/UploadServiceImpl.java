package com.project.module.admin.service.impl;

import com.project.common.exception.BusinessException;
import com.project.common.exception.ErrorCode;
import com.project.module.admin.dto.ImageUploadVO;
import com.project.module.admin.dto.VideoUploadVO;
import com.project.module.admin.service.AdminLogService;
import com.project.module.admin.service.UploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class UploadServiceImpl implements UploadService {

    private final AdminLogService adminLogService;

    // TODO: 待服务器确定后配置上传目录
    // private String uploadDir;

    @Override
    public ImageUploadVO uploadImage(MultipartFile file, Long adminId) {
        if (file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件不能为空");
        }

        // 校验文件类型
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "仅支持图片文件");
        }

        // 校验文件大小（最大10MB）
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "图片大小不能超过10MB");
        }

        // TODO: 实现文件存储上传逻辑
        // 1. 生成UUID文件名
        // 2. 保存到存储服务器（OSS/S3/本地）
        // 3. 生成缩略图
        // 4. 返回访问URL

        ImageUploadVO vo = new ImageUploadVO();
        vo.setUrl(null);        // TODO: 实现上传后填写
        vo.setThumbnailUrl(null);
        vo.setWidth(0);
        vo.setHeight(0);
        vo.setSize(file.getSize());

        adminLogService.log(adminId, "UPLOAD", "IMAGE", file.getOriginalFilename(), null, null);

        log.info("图片上传完成(骨架): originalFilename={}, size={}", file.getOriginalFilename(), file.getSize());
        return vo;
    }

    @Override
    public VideoUploadVO uploadVideo(MultipartFile file, Long adminId) {
        if (file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件不能为空");
        }

        // 校验文件类型
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("video/")) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "仅支持视频文件");
        }

        // TODO: 实现视频上传逻辑
        // 1. 保存原始视频
        // 2. 异步转码为HLS（.m3u8 + .ts）
        // 3. 返回原始URL和播放URL

        VideoUploadVO vo = new VideoUploadVO();
        vo.setUrl(null);        // TODO: 实现上传后填写
        vo.setPlayUrl(null);
        vo.setDuration(0);
        vo.setSize(file.getSize());
        vo.setTranscodeStatus("PENDING"); // 待转码

        adminLogService.log(adminId, "UPLOAD", "VIDEO", file.getOriginalFilename(), null, null);

        log.info("视频上传完成(骨架): originalFilename={}, size={}", file.getOriginalFilename(), file.getSize());
        return vo;
    }
}
