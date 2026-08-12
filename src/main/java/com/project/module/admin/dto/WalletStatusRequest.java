package com.project.module.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "钱包状态修改请求")
public class WalletStatusRequest {
    @NotNull @Schema(description = "状态：1-正常，2-冻结，3-关闭")
    private Integer status;
    @Size(max = 255) @Schema(description = "状态变更原因")
    private String reason;
}
