package com.project.module.entitlement.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.project.module.entitlement.dto.EntitlementVO;
import com.project.module.entitlement.dto.MyEntitlementVO;
import com.project.module.entitlement.entity.UserEntitlement;

import java.util.List;

public interface EntitlementService extends IService<UserEntitlement> {

    /**
     * 检查用户是否有权限访问某剧集
     */
    boolean checkAccess(Long userId, Long contentId, Long episodeId);

    /**
     * 获取用户所有权益
     */
    List<EntitlementVO> listUserEntitlements(Long userId);

    /**
     * 分页获取用户购买的短剧/影游权益
     */
    Page<MyEntitlementVO> pageMyEntitlements(Long userId, Integer contentType, Integer pageNum, Integer size);

    Page<EntitlementVO> pageAdminEntitlements(Long userId, Integer type, Integer pageNum, Integer size);
    EntitlementVO grantByProduct(Long userId, Long productId, Long adminId);
    void revoke(Long entitlementId, Long adminId);

    /**
     * 支付成功后发放权益
     */
    UserEntitlement grant(Long userId, Long productId);
}
