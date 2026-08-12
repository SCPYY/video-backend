package com.project.module.admin.service;
import com.project.module.admin.dto.AdminMenuVO;
import java.util.List;
public interface AdminMenuService { List<AdminMenuVO> menus(String role); }
