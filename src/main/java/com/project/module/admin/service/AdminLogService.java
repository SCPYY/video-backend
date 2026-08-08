package com.project.module.admin.service;

public interface AdminLogService {

    /**
     * 记录操作日志
     *
     * @param adminId    管理员ID
     * @param action     操作类型：CREATE/UPDATE/DELETE/UPLOAD/BATCH_CREATE
     * @param module     操作模块：CONTENT/EPISODE/UPLOAD/IMAGE
     * @param targetId   操作目标ID
     * @param beforeData 修改前数据（JSON），可为null
     * @param afterData  修改后数据（JSON），可为null
     */
    void log(Long adminId, String action, String module, String targetId,
             Object beforeData, Object afterData);
}
