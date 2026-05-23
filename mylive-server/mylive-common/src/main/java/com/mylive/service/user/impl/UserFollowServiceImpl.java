package com.mylive.service.user.impl;

import com.mylive.constants.Constants;
import com.mylive.enums.MessageTypeEnum;
import com.mylive.enums.MessageUpsertTypeEnum;
import com.mylive.enums.StatisticTypeEnum;
import com.mylive.exception.BusinessException;
import com.mylive.infra.jpa.entity.dto.TokenInfo;
import com.mylive.infra.jpa.entity.dto.UserMessageExtendDto;
import com.mylive.infra.jpa.entity.po.UserFollow;
import com.mylive.infra.jpa.entity.po.UserMessage;
import com.mylive.infra.jpa.entity.vo.FollowOrFanListVo;
import com.mylive.infra.jpa.entity.vo.PaginationResultVo;
import com.mylive.infra.jpa.repository.UserFollowRepository;
import com.mylive.infra.jpa.repository.UserInfoRepository;
import com.mylive.infra.redis.RedisComponent;
import com.mylive.response.ResponseCodeEnum;
import com.mylive.service.user.UserFollowService;
import com.mylive.service.user.UserMessageService;
import com.mylive.utils.StringTools;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserFollowServiceImpl implements UserFollowService {

    private final UserInfoRepository userInfoRepository;
    private final UserFollowRepository userFollowRepository;
    private final UserMessageService userMessageService;
    private final RedisComponent redisComponent;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void followUser(TokenInfo tokenInfo, Long followUserId) {
        Long userId = tokenInfo.getUserId();
        if (userId.equals(followUserId)) {
            throw new BusinessException("You can't follow yourself");
        }
        userInfoRepository.findById(followUserId)
                .orElseThrow(() -> new BusinessException(ResponseCodeEnum.BAD_REQUEST));

        UserFollow userFollow = new UserFollow();
        userFollow.setUserId(userId);
        userFollow.setFollowUserId(followUserId);

        // 数据库有唯一键，直接 flush，重复键值异常会被全局捕获
        userFollowRepository.saveAndFlush(userFollow);

        UserMessage userMessage = new UserMessage();
        userMessage.setUserId(followUserId);      // 被关注的人收到消息
        userMessage.setSendUserId(userId);        // 发起关注的人
        userMessage.setMessageType(MessageTypeEnum.USER_FOLLOW.getType());
        UserMessageExtendDto dto = new UserMessageExtendDto();
        dto.setSendUserNickname(tokenInfo.getNickname());
        dto.setSendUserAvatar(tokenInfo.getAvatar());

        userMessageService.saveOrUpdateMessage(userMessage, dto, MessageUpsertTypeEnum.FOLLOW);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                redisComponent.cleanUserCountInfo(userId);
                redisComponent.cleanUserCountInfo(followUserId);
                redisComponent.updateStatisticFanCount(followUserId, 1, StatisticTypeEnum.FAN);
            }
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unFollow(Long userId, Long followUserId) {
        if (userId.equals(followUserId)) {
            throw new BusinessException("You can't unfollow yourself");
        }
        int deleted = userFollowRepository.deleteByUserIdAndFollowUserId(userId, followUserId);
        if (deleted == 0) {
            throw new BusinessException(ResponseCodeEnum.BAD_REQUEST);
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                redisComponent.cleanUserCountInfo(userId);
                redisComponent.cleanUserCountInfo(followUserId);
                redisComponent.updateStatisticFanCount(followUserId, -1, StatisticTypeEnum.FAN);
            }
        });
    }

    @Override
    public PaginationResultVo<FollowOrFanListVo> findFollowPage(Integer pageNo, Long userId) {
        if (pageNo == null || pageNo < 1) pageNo = 1;
        Pageable pageable = PageRequest.of(pageNo - 1,
                Constants.PAGE_SIZE, Sort.by(Sort.Order.desc("followAt")));
        Page<FollowOrFanListVo> page = userFollowRepository.getFollowPage(userId, pageable);
        return PaginationResultVo.fromPage(page);
    }

    @Override
    public PaginationResultVo<FollowOrFanListVo> findFanPage(Integer pageNo, Long userId) {
        if (pageNo == null || pageNo < 1) pageNo = 1;
        Pageable pageable = PageRequest.of(pageNo - 1,
                Constants.PAGE_SIZE, Sort.by(Sort.Order.desc("followAt")));
        Page<FollowOrFanListVo> page = userFollowRepository.getFanPage(userId, pageable);
        return PaginationResultVo.fromPage(page);
    }

    @Override
    public List<FollowOrFanListVo> searchMentionUserList(String keyword, Long userId) {
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Order.desc("followAt")));
        if (keyword != null && keyword.length() > 20) {
            keyword = keyword.substring(0, 20);
        }
        return userFollowRepository.searchMentionUserList(
                userId, StringTools.normalizeKeyword(keyword), pageable);
    }
}
