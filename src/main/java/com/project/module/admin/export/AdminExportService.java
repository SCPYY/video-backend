package com.project.module.admin.export;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

public interface AdminExportService {
    void export(String resource, String format, Map<String, String> filters,
                HttpServletResponse response, Long operatorId) throws IOException;
}
