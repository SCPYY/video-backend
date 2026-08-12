package com.project.module.admin.service.impl;

import com.project.module.admin.dto.AdminMenuVO;
import com.project.module.admin.service.AdminMenuService;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class AdminMenuServiceImpl implements AdminMenuService {
    private record Item(String id,String parent,String name,String title,String type,String route,String component,String icon,String permission,int sort,Set<String> roles) {}
    private static final List<Item> ITEMS = List.of(
        new Item("dashboard",null,"Dashboard","数据看板","PAGE","/dashboard","Layout","Odometer","dashboard:view",10,Set.of("SUPER_ADMIN","ADMIN","OPERATOR")),
        new Item("profile",null,"Profile","个人中心","PAGE","/profile","Profile","User","profile:view",5,Set.of("SUPER_ADMIN","ADMIN","OPERATOR")),
        new Item("content",null,"Content","内容管理","GROUP",null,"Layout","VideoCamera","content:view",20,Set.of("SUPER_ADMIN","ADMIN","OPERATOR")),
        new Item("content-list","content","ContentList","内容列表","PAGE","/content/list","ContentList","List","content:view",10,Set.of("SUPER_ADMIN","ADMIN","OPERATOR")),
        new Item("content-detail","content","ContentDetail","内容详情","PAGE","/content/detail/:id","ContentDetail","View","content:view",13,Set.of("SUPER_ADMIN","ADMIN","OPERATOR")),
        new Item("content-statistics","content","ContentStatistics","内容统计","PAGE","/content/statistics/:id","ContentStatistics","DataAnalysis","content:view",14,Set.of("SUPER_ADMIN","ADMIN","OPERATOR")),
        new Item("content-edit","content","ContentEdit","编辑内容","PAGE","/content/edit/:id","ContentEdit","Edit","content:edit",15,Set.of("SUPER_ADMIN","ADMIN","OPERATOR")),
        new Item("content-create-drama","content","ContentCreateDrama","创建短剧","PAGE","/content/create/drama","ContentCreateDrama","Plus","content:create",11,Set.of("SUPER_ADMIN","ADMIN","OPERATOR")),
        new Item("content-create-interactive","content","ContentCreateInteractive","创建影游","PAGE","/content/create/interactive","ContentCreateInteractive","Plus","content:create",12,Set.of("SUPER_ADMIN","ADMIN","OPERATOR")),
        new Item("content-category","content","ContentCategory","内容分类","PAGE","/content/categories","ContentCategories","CollectionTag","content:category",30,Set.of("SUPER_ADMIN","ADMIN","OPERATOR")),
        new Item("user",null,"User","用户管理","PAGE","/user/list","UserList","User","user:view",30,Set.of("SUPER_ADMIN","ADMIN","OPERATOR")),
        new Item("order",null,"Order","订单管理","PAGE","/order/list","OrderList","Tickets","order:view",40,Set.of("SUPER_ADMIN","ADMIN","OPERATOR")),
        new Item("wallet",null,"Wallet","钱包管理","PAGE","/wallet/list","WalletList","Wallet","wallet:view",50,Set.of("SUPER_ADMIN","ADMIN","OPERATOR")),
        new Item("entitlement",null,"Entitlement","权益管理","PAGE","/entitlement/list","EntitlementList","Key","entitlement:view",60,Set.of("SUPER_ADMIN","ADMIN","OPERATOR")),
        new Item("system",null,"System","系统管理","GROUP",null,"Layout","Setting","system:view",90,Set.of("SUPER_ADMIN")),
        new Item("system-admin", "system","AdminAccount","管理员账号","PAGE","/system/admins","AdminAccount","UserFilled","system:admin",10,Set.of("SUPER_ADMIN")),
        new Item("system-log", "system","AdminLog","操作日志","PAGE","/system/logs","AdminLogs","Document","system:log",20,Set.of("SUPER_ADMIN","ADMIN"))
    );
    @Override public List<AdminMenuVO> menus(String role) {
        String normalized = normalize(role);
        Map<String,AdminMenuVO> map = new LinkedHashMap<>();
        Set<String> hiddenRoutes = Set.of("profile", "content-detail", "content-statistics", "content-edit", "content-create-drama", "content-create-interactive");
        ITEMS.stream().filter(i->i.roles().contains(normalized)).sorted(Comparator.comparing(Item::sort)).forEach(i->{AdminMenuVO v=new AdminMenuVO();v.setId(i.id());v.setParentId(i.parent());v.setName(i.name());v.setTitle(i.title());v.setMenuType(i.type());v.setRoutePath(i.route());v.setComponent(i.component());v.setIcon(i.icon());v.setPermission(i.permission());v.setSortOrder(i.sort());v.setVisible(!hiddenRoutes.contains(i.id()));v.setChildren(new ArrayList<>());map.put(i.id(),v);});
        List<AdminMenuVO> roots=new ArrayList<>(); map.values().forEach(v->{if(v.getParentId()==null) roots.add(v); else if(map.containsKey(v.getParentId())) map.get(v.getParentId()).getChildren().add(v);});
        return roots;
    }
    private String normalize(String role){return switch(role==null?"OPERATOR":role.toUpperCase()){case "ADMIN"->"SUPER_ADMIN";case "EDITOR"->"ADMIN";case "VIEWER"->"OPERATOR";default->role.toUpperCase();};}
}
