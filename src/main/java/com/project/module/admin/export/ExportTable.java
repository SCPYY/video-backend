package com.project.module.admin.export;

import java.util.List;

public record ExportTable(String sheetName, List<String> headers, List<List<String>> rows) {}
