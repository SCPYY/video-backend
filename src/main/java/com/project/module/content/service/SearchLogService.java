package com.project.module.content.service;

import com.project.module.content.dto.HotTagVO;
import com.project.module.content.dto.HotContentVO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

public interface SearchLogService {
    void record(Long userId, String keyword, long resultCount, HttpServletRequest request);
    List<HotTagVO> hotTags(Integer limit, Integer days);
    List<HotContentVO> hotContents(Integer limit);
}
