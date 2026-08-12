package com.project.module.admin.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class DashboardSummaryVO {
    private Long totalUsers;
    private Long onlineContents;
    private Long dramaContents;
    private Long gameContents;
    private Long totalOrders;
    private Long paidOrders;
    private Long pendingOrders;
    private BigDecimal paidAmount;
    private Long pendingReports;
    private Long searchCount;
}
