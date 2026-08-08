package com.project.module.admin.service;

import com.project.module.admin.dto.ImageUploadVO;
import com.project.module.admin.dto.VideoUploadVO;
import org.springframework.web.multipart.MultipartFile;

public interface UploadService {

    /**
     * 上传图片（骨架，待服务器确定后实现）
     */
    ImageUploadVO uploadImage(MultipartFile file, Long adminId);

    /**
     * 上传视频（骨架，待服务器确定后实现）
     */
    VideoUploadVO uploadVideo(MultipartFile file, Long adminId);
}
