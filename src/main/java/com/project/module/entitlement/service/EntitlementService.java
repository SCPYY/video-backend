package com.project.module.entitlement.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.project.module.entitlement.dto.EntitlementVO;
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
     * 支付成功后发放权益
     */
    void grant(Long userId, Long productId);
}
