package com.project.module.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.project.module.product.entity.Product;
import com.project.module.product.mapper.ProductMapper;
import com.project.module.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

    @Override
    public Page<Product> pageProducts(Integer type, Long contentId, Integer pageNum, Integer size) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getStatus, 1);
        wrapper.eq(type != null, Product::getType, type);
        wrapper.eq(contentId != null, Product::getContentId, contentId);
        wrapper.orderByAsc(Product::getType);
        wrapper.orderByDesc(Product::getId);
        return page(new Page<>(pageNum != null ? pageNum : 1, size != null ? size : 20), wrapper);
    }

    @Override
    public List<Product> getByContentId(Long contentId) {
        return list(new LambdaQueryWrapper<Product>()
                .eq(Product::getContentId, contentId)
                .eq(Product::getStatus, 1)
                .orderByAsc(Product::getType));
    }

    @Override
    public List<Product> getMembershipPlans() {
        return list(new LambdaQueryWrapper<Product>()
                .eq(Product::getType, 3)
                .eq(Product::getStatus, 1));
    }
}
