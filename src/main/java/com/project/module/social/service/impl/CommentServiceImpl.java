package com.project.module.social.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.project.common.exception.BusinessException;
import com.project.common.exception.ErrorCode;
import com.project.module.social.dto.CommentRequest;
import com.project.module.social.dto.CommentVO;
import com.project.module.social.dto.LikeResult;
import com.project.module.social.entity.Comment;
import com.project.module.social.entity.CommentLike;
import com.project.module.social.mapper.CommentLikeMapper;
import com.project.module.social.mapper.CommentMapper;
import com.project.module.social.service.CommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService {

    private final CommentLikeMapper commentLikeMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    @Transactional
    public CommentVO publishComment(CommentRequest request, Long userId, String ipAddress) {
        // 1. 评论频率限制 (1分钟最多5条)
        String rateKey = "comment:rate:" + userId;
        String countStr = stringRedisTemplate.opsForValue().get(rateKey);
        int currentCount = countStr != null ? Integer.parseInt(countStr) : 0;
        if (currentCount >= 5) {
            throw new BusinessException(ErrorCode.COMMENT_RATE_LIMIT);
        }
        stringRedisTemplate.opsForValue().increment(rateKey);
        if (currentCount == 0) {
            stringRedisTemplate.expire(rateKey, 60, TimeUnit.SECONDS);
        }

        // 2. 构建评论实体
        Comment comment = new Comment();
        comment.setContentId(request.getContentId());
        comment.setEpisodeId(request.getEpisodeId());
        comment.setUserId(userId);
        comment.setContent(request.getContent());
        comment.setIpAddress(ipAddress);

        Long parentId = request.getParentId() != null ? request.getParentId() : 0L;
        comment.setParentId(parentId);

        // 3. 确定root_id和reply_to_user_id
        if (parentId != 0) {
            Comment parentComment = baseMapper.selectById(parentId);
            if (parentComment == null || parentComment.getStatus() != 1) {
                throw new BusinessException(ErrorCode.COMMENT_NOT_FOUND);
            }
            comment.setRootId(parentComment.getRootId() != 0 ? parentComment.getRootId() : parentId);
            comment.setReplyToUserId(parentComment.getUserId());

            // 父评论回复数+1
            baseMapper.incrementReplyCount(parentId);
        } else {
            comment.setRootId(0L);
        }

        // 4. 保存
        baseMapper.insert(comment);

        // 5. 返回VO
        return toVO(comment, userId);
    }

    @Override
    public Page<CommentVO> getCommentList(Long contentId, Long episodeId, Long userId,
                                          int page, int size, String sort) {
        int offset = (page - 1) * size;

        // 1. 查询一级评论列表
        List<Comment> rootComments = baseMapper.selectRootComments(contentId, episodeId, offset, size, sort);

        // 2. 统计总数
        long total = baseMapper.countRootComments(contentId, episodeId);

        // 3. 批量查询每个一级评论的前3条子回复
        List<Long> rootIds = rootComments.stream()
                .filter(c -> c.getReplyCount() > 0)
                .map(Comment::getId)
                .collect(Collectors.toList());

        Map<Long, List<Comment>> subRepliesMap = Collections.emptyMap();
        if (!rootIds.isEmpty()) {
            List<Comment> allSubReplies = baseMapper.selectSubRepliesByRootIds(rootIds, 3);
            subRepliesMap = allSubReplies.stream()
                    .collect(Collectors.groupingBy(Comment::getRootId, LinkedHashMap::new, Collectors.toList()));
        }

        // 4. 获取当前用户的点赞状态
        final Set<Long> likedIds;
        final Set<Long> dislikedIds;
        if (userId != null) {
            List<Long> allIds = new ArrayList<>(rootIds);
            for (Comment c : rootComments) {
                List<Comment> subs = subRepliesMap.get(c.getId());
                if (!CollectionUtils.isEmpty(subs)) {
                    subs.forEach(s -> allIds.add(s.getId()));
                }
            }
            if (!allIds.isEmpty()) {
                List<CommentLike> likes = commentLikeMapper.selectByUserAndCommentIds(userId, allIds);
                likedIds = likes.stream()
                        .filter(l -> l.getType() == 1)
                        .map(CommentLike::getCommentId)
                        .collect(Collectors.toSet());
                dislikedIds = likes.stream()
                        .filter(l -> l.getType() == 2)
                        .map(CommentLike::getCommentId)
                        .collect(Collectors.toSet());
            } else {
                likedIds = Collections.emptySet();
                dislikedIds = Collections.emptySet();
            }
        } else {
            likedIds = Collections.emptySet();
            dislikedIds = Collections.emptySet();
        }

        // 5. 组装VO
        List<CommentVO> voList = new ArrayList<>();
        for (Comment comment : rootComments) {
            CommentVO vo = toVOWithLike(comment, userId, likedIds, dislikedIds);

            List<Comment> subList = subRepliesMap.getOrDefault(comment.getId(), Collections.emptyList());
            List<CommentVO> subVOList = subList.stream()
                    .map(sub -> toVOWithLike(sub, userId, likedIds, dislikedIds))
                    .collect(Collectors.toList());
            vo.setSubReplies(subVOList);
            vo.setHasMoreSubReplies(comment.getReplyCount() > subList.size());

            voList.add(vo);
        }

        Page<CommentVO> result = new Page<>();
        result.setRecords(voList);
        result.setTotal(total);
        result.setCurrent(page);
        result.setSize(size);
        return result;
    }

    @Override
    public Page<CommentVO> getSubReplies(Long commentId, Long userId, int page, int size) {
        Comment parent = baseMapper.selectById(commentId);
        if (parent == null) {
            throw new BusinessException(ErrorCode.COMMENT_NOT_FOUND);
        }

        long rootId = parent.getRootId() != 0 ? parent.getRootId() : parent.getId();
        int offset = (page - 1) * size;

        // 分页查询子回复
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Comment::getRootId, rootId)
                .ne(Comment::getParentId, 0L)
                .eq(Comment::getStatus, 1)
                .orderByAsc(Comment::getCreatedAt)
                .last("LIMIT " + offset + "," + size);

        List<Comment> subList = baseMapper.selectList(wrapper);

        long total = baseMapper.selectCount(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getRootId, rootId)
                .ne(Comment::getParentId, 0L)
                .eq(Comment::getStatus, 1));

        // 获取点赞状态
        final Set<Long> likedIds2;
        final Set<Long> dislikedIds2;
        if (userId != null && !subList.isEmpty()) {
            List<Long> ids = subList.stream().map(Comment::getId).collect(Collectors.toList());
            List<CommentLike> likes = commentLikeMapper.selectByUserAndCommentIds(userId, ids);
            likedIds2 = likes.stream().filter(l -> l.getType() == 1).map(CommentLike::getCommentId).collect(Collectors.toSet());
            dislikedIds2 = likes.stream().filter(l -> l.getType() == 2).map(CommentLike::getCommentId).collect(Collectors.toSet());
        } else {
            likedIds2 = Collections.emptySet();
            dislikedIds2 = Collections.emptySet();
        }

        List<CommentVO> records = subList.stream()
                .map(c -> toVOWithLike(c, userId, likedIds2, dislikedIds2))
                .collect(Collectors.toList());

        Page<CommentVO> result = new Page<>();
        result.setRecords(records);
        result.setTotal(total);
        result.setCurrent(page);
        result.setSize(size);
        return result;
    }

    @Override
    @Transactional
    public void editComment(Long commentId, Long userId, String content) {
        Comment comment = baseMapper.selectById(commentId);
        if (comment == null || comment.getStatus() != 1) {
            throw new BusinessException(ErrorCode.COMMENT_NOT_FOUND);
        }
        if (!comment.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ADMIN_FORBIDDEN);
        }
        comment.setContent(content);
        baseMapper.updateById(comment);
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId, Long userId, boolean isAdmin) {
        Comment comment = baseMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException(ErrorCode.COMMENT_NOT_FOUND);
        }
        if (!isAdmin && !comment.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ADMIN_FORBIDDEN);
        }
        // 软删除
        comment.setStatus(2);
        baseMapper.updateById(comment);
    }

    @Override
    @Transactional
    public LikeResult toggleLike(Long commentId, Long userId) {
        return toggleLikeInternal(commentId, userId, 1);
    }

    @Override
    @Transactional
    public LikeResult toggleDislike(Long commentId, Long userId) {
        return toggleLikeInternal(commentId, userId, 2);
    }

    // --- 私有方法 ---

    private LikeResult toggleLikeInternal(Long commentId, Long userId, int type) {
        Comment comment = baseMapper.selectById(commentId);
        if (comment == null || comment.getStatus() != 1) {
            throw new BusinessException(ErrorCode.COMMENT_NOT_FOUND);
        }

        CommentLike existing = commentLikeMapper.selectByCommentAndUser(commentId, userId);

        if (existing != null) {
            if (existing.getType() == type) {
                // 相同操作 → 取消
                commentLikeMapper.deleteById(existing.getId());
                if (type == 1) {
                    baseMapper.decrementLikeCount(commentId);
                    comment.setLikeCount(Math.max(0, comment.getLikeCount() - 1));
                } else {
                    baseMapper.decrementDislikeCount(commentId);
                    comment.setDislikeCount(Math.max(0, comment.getDislikeCount() - 1));
                }
                return new LikeResult(false,
                        type == 1 ? comment.getLikeCount() : comment.getDislikeCount());
            } else {
                // 切换操作
                existing.setType(type);
                commentLikeMapper.updateById(existing);
                if (type == 1) {
                    baseMapper.incrementLikeCount(commentId);
                    baseMapper.decrementDislikeCount(commentId);
                    comment.setLikeCount(comment.getLikeCount() + 1);
                    comment.setDislikeCount(Math.max(0, comment.getDislikeCount() - 1));
                } else {
                    baseMapper.incrementDislikeCount(commentId);
                    baseMapper.decrementLikeCount(commentId);
                    comment.setDislikeCount(comment.getDislikeCount() + 1);
                    comment.setLikeCount(Math.max(0, comment.getLikeCount() - 1));
                }
                return new LikeResult(true,
                        type == 1 ? comment.getLikeCount() : comment.getDislikeCount());
            }
        } else {
            // 新增
            CommentLike like = new CommentLike();
            like.setCommentId(commentId);
            like.setUserId(userId);
            like.setType(type);
            commentLikeMapper.insert(like);

            if (type == 1) {
                baseMapper.incrementLikeCount(commentId);
                comment.setLikeCount(comment.getLikeCount() + 1);
            } else {
                baseMapper.incrementDislikeCount(commentId);
                comment.setDislikeCount(comment.getDislikeCount() + 1);
            }
            return new LikeResult(true,
                    type == 1 ? comment.getLikeCount() : comment.getDislikeCount());
        }
    }

    private CommentVO toVO(Comment comment, Long currentUserId) {
        CommentVO vo = new CommentVO();
        BeanUtils.copyProperties(comment, vo);
        return vo;
    }

    private CommentVO toVOWithLike(Comment comment, Long currentUserId,
                                   Set<Long> likedIds, Set<Long> dislikedIds) {
        CommentVO vo = new CommentVO();
        BeanUtils.copyProperties(comment, vo);
        if (currentUserId != null) {
            vo.setLiked(likedIds.contains(comment.getId()));
            vo.setDisliked(dislikedIds.contains(comment.getId()));
        } else {
            vo.setLiked(false);
            vo.setDisliked(false);
        }
        return vo;
    }
}
