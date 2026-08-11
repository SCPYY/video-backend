package com.project.module.content.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.project.module.content.dto.ContentDetailVO;
import com.project.module.content.dto.ContentListItemVO;
import com.project.module.content.dto.ContentListQuery;
import com.project.module.content.entity.Content;

import java.util.List;

public interface ContentService extends IService<Content> {

    /** 内容列表（分页+筛选） */
    Page<ContentListItemVO> pageContent(ContentListQuery query);

    /** 热门内容 */
    List<ContentListItemVO> hotContent(Integer limit);

    /** 首页轮播：最热短剧 Top6 */
    List<ContentListItemVO> carouselContent();

    /** 内容详情（含扩展属性） */
    ContentDetailVO getDetail(Long id);
}
