package com.project.module.product.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.project.module.product.entity.Product;

import java.util.List;

public interface ProductService extends IService<Product> {

    /**
     * 商品列表（分页，按类型/内容筛选）
     */
    Page<Product> pageProducts(Integer type, Long contentId, Integer page, Integer size);

    /**
     * 某内容的可购商品
     */
    List<Product> getByContentId(Long contentId);

    /**
     * 会员商品
     */
    List<Product> getMembershipPlans();
}
