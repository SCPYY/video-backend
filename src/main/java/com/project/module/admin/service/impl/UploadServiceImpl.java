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
import org.springframework.beans.factory.annotation.Value;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UploadServiceImpl implements UploadService {

    private final AdminLogService adminLogService;

    @Value("${app.upload.root:uploads}")
    private String uploadRoot;

    @Value("${app.media.ffprobe-path:ffprobe}")
    private String ffprobePath;

    private static final Set<String> IMAGE_TYPES = Set.of("jpg", "jpeg", "png", "webp", "gif");
    private static final Set<String> VIDEO_TYPES = Set.of("mp4", "mkv", "mov", "webm", "avi");

    // TODO: 待服务器确定后配置上传目录
    // private String uploadDir;

    @Override
    public ImageUploadVO uploadImage(MultipartFile file, Long adminId) {
        if (file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件不能为空");
        }

        String extension = extension(file.getOriginalFilename());
        if (!IMAGE_TYPES.contains(extension)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "仅支持图片文件");
        }

        // 校验文件大小（最大10MB）
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "图片大小不能超过10MB");
        }

        String filename = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        save(file, "images", filename);
        String url = "/uploads/images/" + filename;

        ImageUploadVO vo = new ImageUploadVO();
        vo.setUrl(url);
        vo.setThumbnailUrl(url);
        vo.setWidth(0);
        vo.setHeight(0);
        vo.setSize(file.getSize());

        adminLogService.log(adminId, "UPLOAD", "IMAGE", file.getOriginalFilename(), null, null);

        log.info("图片上传完成: originalFilename={}, size={}, url={}", file.getOriginalFilename(), file.getSize(), url);
        return vo;
    }

    @Override
    public VideoUploadVO uploadVideo(MultipartFile file, Long adminId) {
        if (file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件不能为空");
        }

        String extension = extension(file.getOriginalFilename());
        if (!VIDEO_TYPES.contains(extension)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "仅支持视频文件");
        }

        String filename = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        save(file, "videos", filename);
        String url = "/uploads/videos/" + filename;
        Path savedFile = Paths.get(uploadRoot).toAbsolutePath().normalize().resolve("videos").resolve(filename);
        Integer duration = probeDuration(savedFile);

        VideoUploadVO vo = new VideoUploadVO();
        vo.setUrl(url);
        vo.setPlayUrl(url);
        vo.setDuration(duration);
        vo.setSize(file.getSize());
        vo.setTranscodeStatus("PENDING");

        adminLogService.log(adminId, "UPLOAD", "VIDEO", file.getOriginalFilename(), null, null);

        log.info("视频上传完成: originalFilename={}, size={}, url={}", file.getOriginalFilename(), file.getSize(), url);
        return vo;
    }

    private Integer probeDuration(Path file) {
        List<String> command = new ArrayList<>();
        command.add(ffprobePath);
        command.add("-v"); command.add("error");
        command.add("-show_entries"); command.add("format=duration");
        command.add("-of"); command.add("default=noprint_wrappers=1:nokey=1");
        command.add(file.toString());
        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            String output;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                output = reader.lines().findFirst().orElse("").trim();
            }
            int exitCode = process.waitFor();
            if (exitCode != 0 || output.isBlank()) {
                log.warn("无法识别视频时长: file={}, ffprobePath={}, ffprobeExitCode={}, output={}", file, ffprobePath, exitCode, output);
                return 0;
            }
            int duration = Math.max(0, (int) Math.ceil(Double.parseDouble(output)));
            log.info("视频时长识别成功: file={}, duration={}s", file, duration);
            return duration;
        } catch (Exception e) {
            log.warn("FFprobe执行失败，请检查app.media.ffprobe-path配置: {}", e.getMessage());
            return 0;
        }
    }

    private void save(MultipartFile file, String folder, String filename) {
        try {
            Path directory = Paths.get(uploadRoot).toAbsolutePath().normalize().resolve(folder).normalize();
            Files.createDirectories(directory);
            Path target = directory.resolve(filename).normalize();
            if (!target.startsWith(directory)) throw new BusinessException(ErrorCode.PARAM_ERROR, "非法文件路径");
            file.transferTo(target);
        } catch (IOException e) {
            log.error("文件保存失败", e);
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件保存失败");
        }
    }

    private String extension(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件扩展名不能为空");
        }
        return originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }
}
