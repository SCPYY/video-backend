package com.project.module.admin.export;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.module.admin.entity.AdminLog;
import com.project.module.admin.mapper.AdminLogMapper;
import com.project.module.content.entity.Content;
import com.project.module.content.mapper.ContentMapper;
import com.project.module.entitlement.entity.UserEntitlement;
import com.project.module.entitlement.mapper.EntitlementMapper;
import com.project.module.order.entity.Order;
import com.project.module.order.mapper.OrderMapper;
import com.project.module.product.entity.Product;
import com.project.module.product.mapper.ProductMapper;
import com.project.module.user.entity.SysUser;
import com.project.module.user.mapper.UserMapper;
import com.project.module.wallet.entity.UserWallet;
import com.project.module.wallet.entity.WalletTransaction;
import com.project.module.wallet.mapper.UserWalletMapper;
import com.project.module.wallet.mapper.WalletTransactionMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminExportServiceImpl implements AdminExportService {
    private final ObjectMapper objectMapper;
    private final UserMapper userMapper;
    private final ContentMapper contentMapper;
    private final OrderMapper orderMapper;
    private final ProductMapper productMapper;
    private final EntitlementMapper entitlementMapper;
    private final UserWalletMapper walletMapper;
    private final WalletTransactionMapper transactionMapper;
    private final AdminLogMapper logMapper;

    @Override
    public void export(String resource, String format, Map<String, String> filters,
                       HttpServletResponse response, Long operatorId) throws IOException {
        String f = format == null ? "csv" : format.toLowerCase(Locale.ROOT);
        if (!Set.of("csv", "json", "xlsx", "pdf").contains(f)) { response.sendError(400, "format仅支持csv、json、xlsx、pdf"); return; }
        ExportTable table = load(resource.toLowerCase(Locale.ROOT), filters);
        String filename = resource + "-export-" + System.currentTimeMillis() + "." + f;
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(switch (f) { case "json" -> "application/json;charset=UTF-8"; case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"; case "pdf" -> "application/pdf"; default -> "text/csv;charset=UTF-8"; });
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + URLEncoder.encode(filename, StandardCharsets.UTF_8));
        if (f.equals("json")) objectMapper.writeValue(response.getOutputStream(), table.rows().stream().map(r -> toMap(table.headers(), r)).toList());
        else if (f.equals("xlsx")) writeXlsx(table, response);
        else if (f.equals("pdf")) writePdf(table, response);
        else writeCsv(table, response);
    }

    private ExportTable load(String resource, Map<String, String> p) {
        return switch (resource) {
            case "users" -> table("用户", new String[]{"ID","用户名","邮箱","手机号","昵称","状态","最后登录","创建时间"}, userMapper.selectList(new QueryWrapper<SysUser>().last("LIMIT 10000")), u -> List.of(v(u.getId()),v(u.getUsername()),v(u.getEmail()),v(u.getPhone()),v(u.getNickname()),v(u.getStatus()),v(u.getLastLoginTime()),v(u.getCreatedAt())));
            case "contents" -> table("内容", new String[]{"ID","类型","标题","分类","内容状态","浏览量","播放量","点赞数","创建人","创建时间"}, contentMapper.selectList(new QueryWrapper<Content>().ne("content_status",6).last("LIMIT 10000")), c -> List.of(v(c.getId()),v(c.getType()),v(c.getTitle()),v(c.getCategory()),v(c.getContentStatus()),v(c.getViewCount()),v(c.getPlayCount()),v(c.getLikeCount()),v(c.getCreatedBy()),v(c.getCreatedAt())));
            case "orders" -> table("订单", new String[]{"ID","订单号","用户ID","商品ID","金额","币种","支付方式","状态","支付时间","创建时间"}, orderMapper.selectList(new QueryWrapper<Order>().last("LIMIT 10000")), o -> List.of(v(o.getId()),v(o.getOrderNo()),v(o.getUserId()),v(o.getProductId()),v(o.getAmount()),v(o.getCurrency()),v(o.getPaymentMethod()),v(o.getStatus()),v(o.getPaidAt()),v(o.getCreatedAt())));
            case "products" -> table("商品", new String[]{"ID","名称","类型","内容ID","分集ID","USD价格","EUR价格","平台币价格","状态","创建时间"}, productMapper.selectList(new QueryWrapper<Product>().last("LIMIT 10000")), x -> List.of(v(x.getId()),v(x.getName()),v(x.getType()),v(x.getContentId()),v(x.getEpisodeId()),v(x.getPriceUsd()),v(x.getPriceEur()),v(x.getPricePlatformCoin()),v(x.getStatus()),v(x.getCreatedAt())));
            case "entitlements" -> table("权益", new String[]{"ID","用户ID","类型","内容ID","分集ID","过期时间","创建时间"}, entitlementMapper.selectList(new QueryWrapper<UserEntitlement>().last("LIMIT 10000")), x -> List.of(v(x.getId()),v(x.getUserId()),v(x.getType()),v(x.getContentId()),v(x.getEpisodeId()),v(x.getExpireTime()),v(x.getCreatedAt())));
            case "wallets" -> table("钱包", new String[]{"ID","用户ID","币种","可用余额","冻结余额","状态","创建时间"}, walletMapper.selectList(new QueryWrapper<UserWallet>().last("LIMIT 10000")), x -> List.of(v(x.getId()),v(x.getUserId()),v(x.getCurrency()),v(x.getAvailableBalance()),v(x.getFrozenBalance()),v(x.getStatus()),v(x.getCreatedAt())));
            case "wallet-transactions" -> table("钱包流水", new String[]{"ID","流水号","用户ID","类型","方向","金额","变更前","变更后","关联类型","备注","创建时间"}, transactionMapper.selectList(new QueryWrapper<WalletTransaction>().last("LIMIT 10000")), x -> List.of(v(x.getId()),v(x.getTransactionNo()),v(x.getUserId()),v(x.getType()),v(x.getDirection()),v(x.getAmount()),v(x.getBalanceBefore()),v(x.getBalanceAfter()),v(x.getRelatedType()),v(x.getRemark()),v(x.getCreatedAt())));
            case "admin-logs", "logs" -> table("操作日志", new String[]{"ID","管理员ID","动作","模块","目标ID","IP","创建时间"}, logMapper.selectList(new QueryWrapper<AdminLog>().last("LIMIT 10000")), x -> List.of(v(x.getId()),v(x.getAdminId()),v(x.getAction()),v(x.getModule()),v(x.getTargetId()),v(x.getIpAddress()),v(x.getCreatedAt())));
            default -> throw new IllegalArgumentException("不支持的导出资源: " + resource);
        };
    }
    private <T> ExportTable table(String name, String[] headers, List<T> data, Function<T,List<String>> fn) { return new ExportTable(name, List.of(headers), data.stream().map(fn).toList()); }
    private Map<String,String> toMap(List<String> h,List<String> r){Map<String,String> m=new LinkedHashMap<>();for(int i=0;i<h.size();i++)m.put(h.get(i),r.get(i));return m;}
    private String v(Object o){return o==null?"":String.valueOf(o);}
    private String csv(String s){return "\""+s.replace("\"","\"\"").replace("\r"," ").replace("\n"," ")+"\"";}
    private void writeCsv(ExportTable t,HttpServletResponse r)throws IOException{var w=r.getWriter();w.write(t.headers().stream().map(this::csv).collect(Collectors.joining(","))+"\n");for(var row:t.rows())w.write(row.stream().map(this::csv).collect(Collectors.joining(","))+"\n");w.flush();}
    private void writeXlsx(ExportTable t,HttpServletResponse r)throws IOException{try(var wb=new XSSFWorkbook()){Sheet s=wb.createSheet(t.sheetName());Row h=s.createRow(0);for(int i=0;i<t.headers().size();i++)h.createCell(i).setCellValue(t.headers().get(i));int n=1;for(var data:t.rows()){Row row=s.createRow(n++);for(int i=0;i<data.size();i++)row.createCell(i).setCellValue(data.get(i));}for(int i=0;i<t.headers().size();i++)s.autoSizeColumn(i);wb.write(r.getOutputStream());}}
    private void writePdf(ExportTable t,HttpServletResponse r)throws IOException{try(var d=new PDDocument()){PDPage p=new PDPage(PDRectangle.A4);d.addPage(p);PDPageContentStream s=new PDPageContentStream(d,p);s.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA),8);float y=PDRectangle.A4.getHeight()-30;for(var row:t.rows()){if(y<30){s.close();p=new PDPage(PDRectangle.A4);d.addPage(p);s=new PDPageContentStream(d,p);s.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA),8);y=PDRectangle.A4.getHeight()-30;}String line=row.stream().collect(Collectors.joining(" | ")).replaceAll("[^\\x20-\\x7E]","?");s.beginText();s.newLineAtOffset(20,y);s.showText(line.length()>180?line.substring(0,180):line);s.endText();y-=12;}s.close();d.save(r.getOutputStream());}}
}
