package com.mylive.service.video.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mylive.constants.Constants;
import com.mylive.enums.CommentTopTypeEnum;
import com.mylive.enums.MessageTypeEnum;
import com.mylive.enums.StatisticTypeEnum;
import com.mylive.enums.UserActionTypeEnum;
import com.mylive.exception.BusinessException;
import com.mylive.infra.jpa.entity.dto.TokenInfo;
import com.mylive.infra.jpa.entity.dto.UserMessageExtendDto;
import com.mylive.infra.jpa.entity.po.UserAction;
import com.mylive.infra.jpa.entity.po.UserMessage;
import com.mylive.infra.jpa.entity.po.VideoComment;
import com.mylive.infra.jpa.entity.po.VideoInfo;
import com.mylive.infra.jpa.entity.vo.PaginationResultVo;
import com.mylive.infra.jpa.entity.vo.VideoCommentVo;
import com.mylive.infra.jpa.entity.vo.VideoCommentVoItem;
import com.mylive.infra.jpa.repository.UserActionRepository;
import com.mylive.infra.jpa.repository.UserInfoRepository;
import com.mylive.infra.jpa.repository.VideoCommentRepository;
import com.mylive.infra.jpa.repository.VideoInfoRepository;
import com.mylive.infra.mapstruct.VideoCommentMapper;
import com.mylive.infra.redis.RedisComponent;
import com.mylive.response.ResponseCodeEnum;
import com.mylive.service.user.UserMessageService;
import com.mylive.service.video.VideoCommentService;
import com.mylive.utils.StringTools;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoCommentServiceImpl implements VideoCommentService {

    private final VideoInfoRepository videoInfoRepository;
    private final VideoCommentRepository videoCommentRepository;
    private final VideoCommentMapper videoCommentMapper;
    private final RedisComponent redisComponent;
    private final UserActionRepository userActionRepository;
    private final UserMessageService userMessageService;
    private final UserInfoRepository userInfoRepository;
    private final ObjectMapper mapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VideoCommentVoItem postComment(VideoComment comment, Long replyCommentId, String mentionUserIds, Long videoUserId) {
        VideoInfo videoInfo = videoInfoRepository.findById(comment.getVideoId())
                .orElseThrow(() -> new BusinessException(ResponseCodeEnum.BAD_REQUEST));

        if (videoInfo.getAllowComment() != 1) {
            throw new BusinessException("Content creator has disabled comments");
        }

        VideoComment replyComment = null;

        if (replyCommentId != null) {
            replyComment = videoCommentRepository.findByCommentId(replyCommentId);
            if (replyComment == null || !replyComment.getVideoId().equals(comment.getVideoId())) {
                throw new BusinessException(ResponseCodeEnum.BAD_REQUEST);
            }

            Long parentId = replyComment.getParentCommentId() == 0
                    ? replyCommentId
                    : replyComment.getParentCommentId();

            comment.setParentCommentId(parentId);
            comment.setReplyUserId(replyComment.getUserId());
            comment.setReplyNickname(replyComment.getNickname());
            videoCommentRepository.increaseReplyCount(parentId);
        } else {
            comment.setParentCommentId(0L);
        }
        Long receiveUserId;
        Integer messageType;
        String replyContent;

        if (replyCommentId == null) {
            receiveUserId = videoInfo.getUserId();
            messageType = MessageTypeEnum.VIDEO_COMMENT.getType();
            replyContent = null;
        } else {
            receiveUserId = replyComment.getUserId();
            messageType = MessageTypeEnum.COMMENT_REPLY.getType();
            replyContent = replyComment.getContent();
        }

        // 1. 用于 mentionJson：不要过滤自己、不要过滤 receiveUserId
        List<Long> mentionUserIdList = StringTools.parseDelimitedDistinctList(mentionUserIds, ",")
                .stream()
                .map(Long::valueOf)
                .limit(20)
                .toList();

        List<VideoCommentVoItem.MentionUserVo> mentionUsers = new ArrayList<>();

        if (!mentionUserIdList.isEmpty()) {
            mentionUsers = userInfoRepository.findMentionUsers(mentionUserIdList);

            if (mentionUsers.size() != mentionUserIdList.size()) {
                throw new BusinessException(ResponseCodeEnum.BAD_REQUEST);
            }
            String mentionJson = null;
            try {
                mentionJson = mapper.writeValueAsString(mentionUsers);
            } catch (JsonProcessingException e) {
                log.error("Failed to convert mentionUsers into json string: {}", mentionUsers, e);
            }
            comment.setMentionJson(mentionJson);
        }

        // 2. 保存评论
        comment.setVideoUserId(videoInfo.getUserId());
        videoCommentRepository.save(comment);
        videoInfoRepository.incrCommentCount(comment.getVideoId());

        List<VideoCommentVoItem.MentionUserVo> finalMentionUsers = mentionUsers;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                // 3. 更新统计
                redisComponent.updateStatisticCount(videoUserId, comment.getVideoId(), 1, StatisticTypeEnum.COMMENT);

                // 4. 评论 / 回复通知：只有发送方和接收方不是一个人的时候才通知
                if (!Objects.equals(receiveUserId, comment.getUserId())) {
                    UserMessage userMessage = new UserMessage();
                    userMessage.setVideoId(comment.getVideoId());
                    userMessage.setCommentId(comment.getCommentId());
                    userMessage.setUserId(receiveUserId);
                    userMessage.setSendUserId(comment.getUserId());
                    userMessage.setMessageType(messageType);

                    UserMessageExtendDto dto = new UserMessageExtendDto();
                    dto.setMessageContent(comment.getContent());
                    dto.setMessageContentReply(replyContent);
                    dto.setVideoTitle(videoInfo.getVideoTitle());
                    dto.setVideoCover(videoInfo.getVideoCover());
                    dto.setSendUserNickname(comment.getNickname());
                    dto.setSendUserAvatar(comment.getAvatar());
                    userMessageService.saveMessage(userMessage, dto);
                }

                // 5. @用户通知
                if (!finalMentionUsers.isEmpty()) {
                    finalMentionUsers.stream()
                            // @ 和发评人是同一人则不重复通知
                            .filter(user -> !Objects.equals(user.getUserId(), comment.getUserId()))
                            // @ 和接收方是同一人则不重复通知
                            .filter(user -> !Objects.equals(user.getUserId(), receiveUserId))
                            .forEach(mentionUser -> {
                                UserMessage mentionMessage = new UserMessage();
                                mentionMessage.setVideoId(comment.getVideoId());
                                mentionMessage.setCommentId(comment.getCommentId());
                                mentionMessage.setUserId(mentionUser.getUserId());
                                mentionMessage.setSendUserId(comment.getUserId());
                                mentionMessage.setMessageType(MessageTypeEnum.COMMENT_MENTION.getType());

                                UserMessageExtendDto mentionDto = new UserMessageExtendDto();
                                mentionDto.setMessageContent(comment.getContent());
                                mentionDto.setMessageContentReply(replyContent);
                                mentionDto.setVideoTitle(videoInfo.getVideoTitle());
                                mentionDto.setVideoCover(videoInfo.getVideoCover());
                                mentionDto.setSendUserNickname(comment.getNickname());
                                mentionDto.setSendUserAvatar(comment.getAvatar());

                                userMessageService.saveMessage(mentionMessage, mentionDto);
                            });
                }
            }
        });
        VideoCommentVoItem vo = videoCommentMapper.toItemVo(comment);
        vo.setMentionUsers(mentionUsers);
        return vo;
    }

    @Override
    public VideoCommentVo getComments(String videoId, Integer pageNo, Integer orderType, String token) {
        VideoInfo videoInfo = videoInfoRepository.findById(videoId)
                .orElseThrow(() -> new BusinessException(ResponseCodeEnum.BAD_REQUEST));
        if (videoInfo.getAllowComment() != 1) {
            return null;
        }
        if (pageNo == null || pageNo < 1) pageNo = 1;
        Sort sort;
        if (orderType == null || orderType == 0) {
            sort = Sort.by(
                    Sort.Order.desc("topType"),
                    Sort.Order.desc("likeCount"),
                    Sort.Order.desc("commentId")
            );
        } else {
            sort = Sort.by(
                    Sort.Order.desc("topType"),
                    Sort.Order.desc("commentId")
            );
        }
        Pageable pageable = PageRequest.of(pageNo - 1, Constants.PAGE_SIZE, sort);
        Page<VideoCommentVoItem> page = videoCommentRepository.getCommentsWithUser(videoId, pageable);
        fillMentionUsers(page);

        List<UserAction> userActionList = List.of();
        TokenInfo tokenInfo = redisComponent.getTokenInfo(token);
        if (tokenInfo != null) {
            userActionList = userActionRepository.findByVideoIdAndUserIdAndActionTypeIn(
                    videoId,
                    tokenInfo.getUserId(),
                    List.of(
                            UserActionTypeEnum.COMMENT_LIKE.getType(),
                            UserActionTypeEnum.COMMENT_DISLIKE.getType()
                    )
            );
        }
        return new VideoCommentVo(PaginationResultVo.fromPage(page), userActionList);
    }

    private void fillMentionUsers(Page<VideoCommentVoItem> page) {
        page.getContent().forEach(item -> {
            if (!StringUtils.hasText(item.getMentionJson())) {
                item.setMentionUsers(List.of());
                return;
            }
            try {
                List<VideoCommentVoItem.MentionUserVo> mentionUsers = mapper.readValue(
                        item.getMentionJson(),
                        new TypeReference<>() {}
                );
                item.setMentionUsers(mentionUsers);
            } catch (Exception e) {
                log.error("Failed to convert mention json to MentionUserVo, json = {}", item.getMentionJson(), e);
                item.setMentionUsers(List.of());
            }
        });
    }

    @Override
    public VideoCommentVo getReplyList(Long parentId, Integer pageNo, String token) {
        if (pageNo == null || pageNo < 1) pageNo = 1;

        Pageable pageable = PageRequest.of(
                pageNo - 1,
                Constants.PAGE_SIZE,
                Sort.by(Sort.Order.asc("commentId"))
        );

        Page<VideoCommentVoItem> page = videoCommentRepository.getReplyWithUser(parentId, pageable);
        fillMentionUsers(page);

        List<UserAction> userActionList = List.of();

        TokenInfo tokenInfo = redisComponent.getTokenInfo(token);
        if (tokenInfo != null) {
            List<Long> commentIds = page.getContent().stream()
                    .map(VideoCommentVoItem::getCommentId)
                    .toList();

            if (!commentIds.isEmpty()) {
                userActionList = userActionRepository
                        .findByCommentIdInAndUserIdAndActionTypeIn(
                                commentIds,
                                tokenInfo.getUserId(),
                                List.of(
                                        UserActionTypeEnum.COMMENT_LIKE.getType(),
                                        UserActionTypeEnum.COMMENT_DISLIKE.getType()
                                )
                        );
            }
        }
        return new VideoCommentVo(PaginationResultVo.fromPage(page), userActionList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void topComment(Long commentId, Long userId) {
        String videoId = videoCommentRepository.findVideoIdByCommentId(commentId);
        if (videoId == null) {
            throw new BusinessException(ResponseCodeEnum.BAD_REQUEST);
        }

        // 设置当前为 TOP（带权限校验）
        int updated = videoCommentRepository.updateTopTypeByCommentIdAndVideoUserIdAndVideoId(
                CommentTopTypeEnum.TOP.getType(),
                commentId,
                userId,
                videoId
        );

        if (updated == 0) {
            throw new BusinessException(ResponseCodeEnum.BAD_REQUEST);
        }

        // 清掉已有 TOP（不包含当前 TOP）
        videoCommentRepository.clearTopByVideoId(
                videoId,
                CommentTopTypeEnum.NO_TOP.getType(),
                CommentTopTypeEnum.TOP.getType(),
                commentId
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelTopComment(Long commentId, Long userId) {
        int updated = videoCommentRepository.updateTopTypeWithAuth(
                CommentTopTypeEnum.NO_TOP.getType(),
                commentId,
                userId
        );
        if (updated == 0) {
            throw new BusinessException(ResponseCodeEnum.BAD_REQUEST);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(Long commentId, Long userId) {
        VideoComment comment = videoCommentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(ResponseCodeEnum.BAD_REQUEST));
        String videoId = comment.getVideoId();
        Long videoUserId = comment.getVideoUserId();

        int deleted;
        if (userId == null) {  // 管理员操作
            deleted = videoCommentRepository.deleteByCommentId(commentId);
        } else {  // 用户操作
            deleted = videoCommentRepository.deleteByCommentIdAndAuth(commentId, userId);
        }

        if (deleted == 0) {
            throw new BusinessException(ResponseCodeEnum.BAD_REQUEST);
        }

        Long parentCommentId = comment.getParentCommentId();

        int decrCount;
        if (parentCommentId == 0) {
            int replyDeleted = videoCommentRepository.deleteByParentCommentId(commentId);
            decrCount = replyDeleted + 1;
        } else {
            if (0 == videoCommentRepository.decrReplyCount(parentCommentId)) {
                throw new BusinessException(ResponseCodeEnum.BAD_REQUEST);
            }
            decrCount = 1;
        }

        if (0 == videoInfoRepository.decrCommentCount(videoId, decrCount)) {
            throw new BusinessException(ResponseCodeEnum.BAD_REQUEST);
        }

        int finalDecrCount = decrCount;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                redisComponent.updateStatisticCount(videoUserId, videoId, -finalDecrCount, StatisticTypeEnum.COMMENT);
            }
        });
    }

    @Override
    public PaginationResultVo<VideoCommentVoItem> getUCenterCommentPage(Integer pageNo, String videoId, Long userId) {
        if (pageNo == null || pageNo < 1) pageNo = 1;
        Pageable pageable = PageRequest.of(
                pageNo - 1,
                Constants.PAGE_SIZE,
                Sort.by(Sort.Order.desc("commentId"))
        );
        Page<VideoCommentVoItem> page = videoCommentRepository.findUCenterCommentPage(userId, videoId, pageable);
        fillMentionUsers(page);
        return PaginationResultVo.fromPage(page);
    }

    @Override
    public PaginationResultVo<VideoCommentVoItem> getAdminCommentPage(Integer pageNo, String videoTitleFuzzy) {
        if (pageNo == null || pageNo < 1) pageNo = 1;
        Pageable pageable = PageRequest.of(
                pageNo - 1,
                Constants.PAGE_SIZE,
                Sort.by(Sort.Order.desc("commentId"))
        );
        Page<VideoCommentVoItem> page = videoCommentRepository.findAdminCommentPage(
                StringTools.normalizeKeyword(videoTitleFuzzy), pageable);
        fillMentionUsers(page);
        return PaginationResultVo.fromPage(page);
    }
}
