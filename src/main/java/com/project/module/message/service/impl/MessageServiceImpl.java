package com.project.module.message.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.project.module.message.entity.SystemMessage;
import com.project.module.message.mapper.SystemMessageMapper;
import com.project.module.message.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
@Service @RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {
 private final SystemMessageMapper mapper;
 public void send(String rt,Long rid,String type,String actionType,String title,String content,String targetType,Long targetId,String targetUrl,String relatedType,Long relatedId){SystemMessage m=new SystemMessage();m.setRecipientType(rt);m.setRecipientId(rid);m.setMessageType(type);m.setActionType(actionType);m.setTitle(title);m.setContent(content);m.setTargetType(targetType);m.setTargetId(targetId);m.setTargetUrl(targetUrl);m.setRelatedType(relatedType);m.setRelatedId(relatedId);m.setIsRead(0);mapper.insert(m);}
 private LambdaQueryWrapper<SystemMessage> scope(String rt,Long rid){return new LambdaQueryWrapper<SystemMessage>().eq(SystemMessage::getRecipientType,rt).and(w->w.eq(SystemMessage::getRecipientId,rid).or().isNull(SystemMessage::getRecipientId));}
 public Page<SystemMessage> page(String rt,Long rid,Integer p,Integer s){return mapper.selectPage(new Page<>(p==null||p<1?1:p,s==null?20:Math.min(100,Math.max(1,s))),scope(rt,rid).orderByDesc(SystemMessage::getId));}
 public long unread(String rt,Long rid){return mapper.selectCount(scope(rt,rid).eq(SystemMessage::getIsRead,0));}
 public void read(Long rid,String rt,Long id){SystemMessage m=mapper.selectById(id);if(m!=null&&rt.equals(m.getRecipientType())&&(m.getRecipientId()==null||rid.equals(m.getRecipientId()))){m.setIsRead(1);m.setReadAt(LocalDateTime.now());mapper.updateById(m);}}
 public void readAll(Long rid,String rt){SystemMessage m=new SystemMessage();m.setIsRead(1);m.setReadAt(LocalDateTime.now());mapper.update(m,scope(rt,rid).eq(SystemMessage::getIsRead,0));}
}
