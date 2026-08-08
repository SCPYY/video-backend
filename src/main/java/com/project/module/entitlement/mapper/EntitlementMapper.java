package com.project.module.entitlement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.project.module.entitlement.entity.UserEntitlement;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EntitlementMapper extends BaseMapper<UserEntitlement> {
}
