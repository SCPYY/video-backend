package com.project.module.admin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.project.module.admin.dto.ContentCreateRequest;
import com.project.module.content.entity.Content;

public interface AdminContentService {

    /** 分页查询内容列表 */
    Page<Content> pageContents(Integer type, Integer status, Integer page, Integer size);

    /** 创建内容（含扩展属性、幂等、标题去重、审计日志） */
    Content createContent(ContentCreateRequest req, Long adminId);

    /** 更新内容（含审计日志、前后快照） */
    Content updateContent(Long id, ContentCreateRequest req, Long adminId);

    /** 删除内容（软删除） */
    void deleteContent(Long id, Long adminId);

    /** 切换上下架状态 */
    void toggleStatus(Long id, Long adminId);
}
