package com.project.module.admin.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.module.admin.entity.AdminLog;
import com.project.module.admin.mapper.AdminLogMapper;
import com.project.module.admin.service.AdminLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminLogServiceImpl implements AdminLogService {

    private final AdminLogMapper adminLogMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void log(Long adminId, String action, String module, String targetId,
                    Object beforeData, Object afterData) {
        try {
            AdminLog adminLog = new AdminLog();
            adminLog.setAdminId(adminId);
            adminLog.setAction(action);
            adminLog.setModule(module);
            adminLog.setTargetId(targetId);
            if (beforeData != null) {
                adminLog.setBeforeData(objectMapper.writeValueAsString(beforeData));
            }
            if (afterData != null) {
                adminLog.setAfterData(objectMapper.writeValueAsString(afterData));
            }
            adminLogMapper.insert(adminLog);
        } catch (Exception e) {
            // 日志记录失败不影响主流程
            log.warn("操作日志记录失败: action={}, module={}", action, module, e);
        }
    }
}
