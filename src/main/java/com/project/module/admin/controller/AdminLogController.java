package com.project.module.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.project.common.response.Result;
import com.project.module.admin.entity.AdminLog;
import com.project.module.admin.mapper.AdminLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

@Tag(name = "管理后台-操作日志", description = "管理员操作审计日志查询")
@RestController
@RequestMapping("/api/v1/admin/logs")
@RequiredArgsConstructor
public class AdminLogController {
    private final AdminLogMapper adminLogMapper;
    private final ObjectMapper objectMapper;

    @GetMapping
    @Operation(summary = "操作日志分页列表", description = "按管理员、模块和动作筛选日志。日志仅供审计查看，不提供删除接口。")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR','VIEWER')")
    public Result<Page<AdminLog>> list(@RequestParam(required = false) Long adminId,
                                       @RequestParam(required = false) String module,
                                       @RequestParam(required = false) String action,
                                       @RequestParam(defaultValue = "1") Integer page,
                                       @RequestParam(defaultValue = "20") Integer size) {
        return Result.ok(adminLogMapper.selectPage(new Page<>(page == null || page < 1 ? 1 : page,
                        size == null ? 20 : Math.min(100, Math.max(1, size))), wrapper(adminId,module,action,null,null)));
    }

    @GetMapping("/export")
    @Operation(summary="导出操作日志", description="支持 csv、xlsx、json、pdf 格式；按管理员、模块、动作和时间范围筛选。单次最多10000条。导出权限：ADMIN、EDITOR、VIEWER，导出行为会写入操作日志。")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR','VIEWER')")
    public void export(@RequestParam(defaultValue="csv") String format,
                       @RequestParam(required=false) Long adminId,
                       @RequestParam(required=false) String module,
                       @RequestParam(required=false) String action,
                       @RequestParam(required=false) LocalDateTime startTime,
                       @RequestParam(required=false) LocalDateTime endTime,
                       HttpServletResponse response) throws IOException {
        String normalized = format.trim().toLowerCase();
        if (!normalized.equals("csv") && !normalized.equals("json") && !normalized.equals("xlsx") && !normalized.equals("pdf")) { response.sendError(400,"format仅支持csv、xlsx、json或pdf"); return; }
        List<AdminLog> logs = adminLogMapper.selectList(wrapper(adminId,module,action,startTime,endTime).last("LIMIT 10000"));
        String filename = "admin-logs-" + System.currentTimeMillis() + "." + normalized;
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(switch (normalized) { case "json" -> "application/json;charset=UTF-8"; case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"; case "pdf" -> "application/pdf"; default -> "text/csv;charset=UTF-8"; });
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + URLEncoder.encode(filename, StandardCharsets.UTF_8));
        if (normalized.equals("json")) {
            objectMapper.writeValue(response.getOutputStream(), logs);
        } else if (normalized.equals("xlsx")) {
            writeXlsx(logs, response);
        } else if (normalized.equals("pdf")) {
            writePdf(logs, response);
        } else {
            var writer = response.getWriter();
            writer.write("ID,管理员ID,动作,模块,目标ID,修改前数据,修改后数据,IP地址,User-Agent,创建时间\n");
            for (AdminLog log : logs) writer.write(csv(log.getId())+","+csv(log.getAdminId())+","+csv(log.getAction())+","+csv(log.getModule())+","+csv(log.getTargetId())+","+csv(log.getBeforeData())+","+csv(log.getAfterData())+","+csv(log.getIpAddress())+","+csv(log.getUserAgent())+","+csv(log.getCreatedAt())+"\n");
            writer.flush();
        }
        // 导出记录本身不影响本次导出文件内容，便于审计导出行为。
        // 这里不递归调用业务接口，直接记录一条 EXPORT 日志。
        adminLogMapper.insert(exportLog(adminId, normalized));
    }

    private LambdaQueryWrapper<AdminLog> wrapper(Long adminId,String module,String action,LocalDateTime start,LocalDateTime end) {
        return new LambdaQueryWrapper<AdminLog>().eq(adminId != null, AdminLog::getAdminId, adminId)
                .eq(module != null && !module.isBlank(), AdminLog::getModule, module)
                .eq(action != null && !action.isBlank(), AdminLog::getAction, action)
                .ge(start != null, AdminLog::getCreatedAt, start).le(end != null, AdminLog::getCreatedAt, end)
                .orderByDesc(AdminLog::getId);
    }
    private String csv(Object value) { if(value==null)return "\"\""; return "\""+String.valueOf(value).replace("\"","\"\"").replace("\r"," ").replace("\n"," ")+"\""; }
    private AdminLog exportLog(Long adminId,String format) { AdminLog l=new AdminLog();l.setAdminId(adminId);l.setAction("EXPORT");l.setModule("ADMIN_LOG");l.setTargetId(format);l.setAfterData("{\"format\":\""+format+"\"}");return l; }

    private void writeXlsx(List<AdminLog> logs, HttpServletResponse response) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("操作日志");
            String[] headers = {"ID","管理员ID","动作","模块","目标ID","修改前数据","修改后数据","IP地址","User-Agent","创建时间"};
            Row header = sheet.createRow(0);
            for (int i=0;i<headers.length;i++) header.createCell(i).setCellValue(headers[i]);
            int rowIndex=1;
            for (AdminLog log:logs) { Row row=sheet.createRow(rowIndex++); Object[] values={log.getId(),log.getAdminId(),log.getAction(),log.getModule(),log.getTargetId(),log.getBeforeData(),log.getAfterData(),log.getIpAddress(),log.getUserAgent(),log.getCreatedAt()}; for(int i=0;i<values.length;i++) row.createCell(i).setCellValue(values[i]==null?"":String.valueOf(values[i])); }
            for(int i=0;i<headers.length;i++) sheet.autoSizeColumn(i);
            workbook.write(response.getOutputStream());
        }
    }

    private void writePdf(List<AdminLog> logs, HttpServletResponse response) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            float y=PDRectangle.A4.getHeight()-40;
            PDPageContentStream stream=new PDPageContentStream(document,page);
            stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA),8);
            stream.beginText(); stream.newLineAtOffset(30,y); stream.showText("Admin Operation Logs"); stream.endText(); y-=18;
            for(AdminLog log:logs){ if(y<30){stream.close(); page=new PDPage(PDRectangle.A4);document.addPage(page);stream=new PDPageContentStream(document,page);stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA),8);y=PDRectangle.A4.getHeight()-30;} String line=(log.getId()+" | admin="+log.getAdminId()+" | "+log.getAction()+" | "+log.getModule()+" | target="+log.getTargetId()+" | "+log.getCreatedAt()); line=line.replaceAll("[^\\x20-\\x7E]","?"); stream.beginText();stream.newLineAtOffset(30,y);stream.showText(line.length()>180?line.substring(0,180):line);stream.endText();y-=12; }
            stream.close(); document.save(response.getOutputStream());
        }
    }
}
