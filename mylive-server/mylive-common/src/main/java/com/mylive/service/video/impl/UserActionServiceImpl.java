package com.mylive.service.video.impl;

import com.mylive.enums.*;
import com.mylive.exception.BusinessException;
import com.mylive.infra.elasticsearch.ElasticSearchComponent;
import com.mylive.infra.jpa.entity.dto.TokenInfo;
import com.mylive.infra.jpa.entity.dto.UserMessageExtendDto;
import com.mylive.infra.jpa.entity.po.UserAction;
import com.mylive.infra.jpa.entity.po.UserMessage;
import com.mylive.infra.jpa.entity.po.VideoComment;
import com.mylive.infra.jpa.entity.po.VideoInfo;
import com.mylive.infra.jpa.repository.UserActionRepository;
import com.mylive.infra.jpa.repository.UserInfoRepository;
import com.mylive.infra.jpa.repository.VideoCommentRepository;
import com.mylive.infra.jpa.repository.VideoInfoRepository;
import com.mylive.infra.redis.RedisComponent;
import com.mylive.response.ResponseCodeEnum;
import com.mylive.service.user.UserMessageService;
import com.mylive.service.video.UserActionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UserActionServiceImpl implements UserActionService {

    private final VideoInfoRepository videoInfoRepository;
    private final UserActionRepository userActionRepository;
    private final UserInfoRepository userInfoRepository;
    private final VideoCommentRepository videoCommentRepository;
    private final ElasticSearchComponent elasticSearchComponent;
    private final UserMessageService userMessageService;
    private final RedisComponent redisComponent;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveAction(UserAction bean, TokenInfo tokenInfo) {
        VideoInfo videoInfo = videoInfoRepository.findByVideoId(bean.getVideoId());
        if (videoInfo == null) {
            throw new BusinessException(ResponseCodeEnum.BAD_REQUEST);
        }
        Long videoUserId = videoInfo.getUserId();
        Long currentUserId = bean.getUserId();
        bean.setVideoUserId(videoUserId);

        UserActionTypeEnum actionTypeEnum = UserActionTypeEnum.getByType(bean.getActionType());
        if (actionTypeEnum == null) {
            throw new BusinessException(ResponseCodeEnum.BAD_REQUEST);
        }

        UserAction dbAction = userActionRepository
                .findByVideoIdAndCommentIdAndActionTypeAndUserId(
                        bean.getVideoId(),
                        bean.getCommentId(),
                        bean.getActionType(),
                        currentUserId
                );

        bean.setActionTime(LocalDateTime.now());

        switch (actionTypeEnum) {
            case VIDEO_LIKE:
            case VIDEO_SAVE:
                if (dbAction != null) {
                    userActionRepository.deleteById(dbAction.getActionId());
                } else {
                    userActionRepository.save(bean);

                    if (!Objects.equals(videoUserId, currentUserId)) {
                        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                            @Override
                            public void afterCommit() {
                                UserMessage userMessage = new UserMessage();
                                userMessage.setVideoId(bean.getVideoId());
                                userMessage.setUserId(videoUserId);
                                userMessage.setSendUserId(currentUserId);

                                if (actionTypeEnum == UserActionTypeEnum.VIDEO_LIKE) {
                                    userMessage.setMessageType(MessageTypeEnum.VIDEO_LIKE.getType());
                                } else {
                                    userMessage.setMessageType(MessageTypeEnum.VIDEO_SAVE.getType());
                                }

                                UserMessageExtendDto dto = new UserMessageExtendDto();
                                dto.setVideoTitle(videoInfo.getVideoTitle());
                                dto.setVideoCover(videoInfo.getVideoCover());
                                dto.setSendUserNickname(tokenInfo.getNickname());
                                dto.setSendUserAvatar(tokenInfo.getAvatar());
                                userMessageService.saveOrUpdateMessage(userMessage, dto, MessageUpsertTypeEnum.VIDEO);
                            }
                        });
                    }
                }

                int changeCount = dbAction == null ? 1 : -1;

                if (actionTypeEnum == UserActionTypeEnum.VIDEO_SAVE) {
                    if (0 == videoInfoRepository.updateSaveCountByVideoId(changeCount, bean.getVideoId())) {
                        throw new BusinessException(ResponseCodeEnum.BAD_REQUEST);
                    }

                    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            redisComponent.updateStatisticCount(videoUserId, bean.getVideoId(), changeCount, StatisticTypeEnum.SAVE);
                            try {
                                elasticSearchComponent.updateDocCount(
                                        bean.getVideoId(),
                                        SearchOrderTypeEnum.VIDEO_SAVE.getField(),
                                        changeCount
                                );
                            } catch (Exception ignores) {}
                        }
                    });
                } else {
                    if (0 == videoInfoRepository.updateLikeCountByVideoId(changeCount, bean.getVideoId())) {
                        throw new BusinessException(ResponseCodeEnum.BAD_REQUEST);
                    }
                    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            redisComponent.updateStatisticCount(videoUserId, bean.getVideoId(), changeCount, StatisticTypeEnum.LIKE);
                        }
                    });
                }
                break;

            case VIDEO_COIN:
                if (Objects.equals(videoUserId, currentUserId)) {
                    throw new BusinessException("You can't coin your own video");
                }
                if (dbAction != null) {
                    throw new BusinessException("Already coined this video");
                }

                if (0 == userInfoRepository.decrCoinCount(currentUserId, bean.getActionCount())) {
                    throw new BusinessException("You don't have enough coin");
                }
                if (0 == userInfoRepository.increaseCoinCount(videoUserId, bean.getActionCount())) {
                    throw new BusinessException(ResponseCodeEnum.BAD_REQUEST);
                }

                userActionRepository.save(bean);

                if (0 == videoInfoRepository.incrCoinCount(bean.getVideoId(), bean.getActionCount())) {
                    throw new BusinessException(ResponseCodeEnum.BAD_REQUEST);
                }

                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        UserMessage coinMessage = new UserMessage();
                        coinMessage.setVideoId(bean.getVideoId());
                        coinMessage.setUserId(videoUserId);
                        coinMessage.setSendUserId(currentUserId);
                        coinMessage.setMessageType(MessageTypeEnum.VIDEO_COIN.getType());

                        UserMessageExtendDto coinDto = new UserMessageExtendDto();
                        coinDto.setVideoTitle(videoInfo.getVideoTitle());
                        coinDto.setVideoCover(videoInfo.getVideoCover());
                        coinDto.setCoinCount(bean.getActionCount());
                        coinDto.setSendUserNickname(tokenInfo.getNickname());
                        coinDto.setSendUserAvatar(tokenInfo.getAvatar());

                        userMessageService.saveMessage(coinMessage, coinDto);

                        redisComponent.cleanUserCountInfo(videoUserId);
                        redisComponent.cleanUserCountInfo(currentUserId);
                        redisComponent.updateStatisticCount(
                                videoUserId,
                                bean.getVideoId(),
                                bean.getActionCount(),
                                StatisticTypeEnum.COIN
                        );
                    }
                });
                break;

            case COMMENT_LIKE:
            case COMMENT_DISLIKE:
                UserActionTypeEnum oppositeEnum = UserActionTypeEnum.COMMENT_LIKE == actionTypeEnum
                        ? UserActionTypeEnum.COMMENT_DISLIKE
                        : UserActionTypeEnum.COMMENT_LIKE;

                UserAction oppositeAction = userActionRepository
                        .findByVideoIdAndCommentIdAndActionTypeAndUserId(
                                bean.getVideoId(),
                                bean.getCommentId(),
                                oppositeEnum.getType(),
                                currentUserId
                        );

                if (oppositeAction != null) {
                    userActionRepository.deleteById(oppositeAction.getActionId());
                }

                if (dbAction != null) {
                    userActionRepository.deleteById(dbAction.getActionId());
                } else {
                    userActionRepository.save(bean);
                }

                int likeDelta = 0;
                int dislikeDelta = 0;

                if (actionTypeEnum == UserActionTypeEnum.COMMENT_LIKE) {
                    likeDelta = dbAction == null ? 1 : -1;
                } else {
                    dislikeDelta = dbAction == null ? 1 : -1;
                }

                if (oppositeAction != null) {
                    if (oppositeEnum == UserActionTypeEnum.COMMENT_LIKE) {
                        likeDelta -= 1;
                    } else {
                        dislikeDelta -= 1;
                    }
                }

                if (0 == videoCommentRepository.updateCount(bean.getCommentId(), likeDelta, dislikeDelta)) {
                    throw new BusinessException(ResponseCodeEnum.BAD_REQUEST);
                }

                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        if (actionTypeEnum == UserActionTypeEnum.COMMENT_LIKE && dbAction == null) {
                            VideoComment comment = videoCommentRepository.findByCommentId(bean.getCommentId());

                            if (comment != null && !Objects.equals(comment.getUserId(), currentUserId)) {
                                UserMessage commentLikeMessage = new UserMessage();
                                commentLikeMessage.setVideoId(bean.getVideoId());
                                commentLikeMessage.setUserId(comment.getUserId());
                                commentLikeMessage.setSendUserId(currentUserId);
                                commentLikeMessage.setMessageType(MessageTypeEnum.COMMENT_LIKE.getType());
                                commentLikeMessage.setCommentId(bean.getCommentId());

                                UserMessageExtendDto commentLikeDto = new UserMessageExtendDto();
                                commentLikeDto.setMessageContent(comment.getContent());
                                commentLikeDto.setVideoTitle(videoInfo.getVideoTitle());
                                commentLikeDto.setVideoCover(videoInfo.getVideoCover());
                                commentLikeDto.setSendUserNickname(tokenInfo.getNickname());
                                commentLikeDto.setSendUserAvatar(tokenInfo.getAvatar());
                                userMessageService.saveOrUpdateMessage(commentLikeMessage, commentLikeDto, MessageUpsertTypeEnum.COMMENT);
                            }
                        }
                    }
                });
                break;
        }
    }
}
