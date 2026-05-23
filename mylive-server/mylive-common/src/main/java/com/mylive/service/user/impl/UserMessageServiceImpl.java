package com.mylive.service.user.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mylive.constants.Constants;
import com.mylive.enums.MessageReadTypeEnum;
import com.mylive.enums.MessageUpsertTypeEnum;
import com.mylive.infra.jpa.entity.dto.UserMessageExtendDto;
import com.mylive.infra.jpa.entity.po.UserMessage;
import com.mylive.infra.jpa.entity.vo.PaginationResultVo;
import com.mylive.infra.jpa.entity.vo.UserMessageCountVo;
import com.mylive.infra.jpa.repository.UserMessageRepository;
import com.mylive.service.user.UserMessageService;
import com.mylive.utils.StringTools;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserMessageServiceImpl implements UserMessageService {

    private final UserMessageRepository repo;
    private final ObjectMapper mapper;

    @Override
    @Async
    @Transactional(rollbackFor = Exception.class)
    public void saveMessage(UserMessage userMessage, UserMessageExtendDto dto) {
        String json = null;
        try {
            json = mapper.writeValueAsString(dto);
        } catch (JsonProcessingException e) {
            log.error("Failed to convert UserMessageExtendDto to json string: {}", dto, e);
        }
        userMessage.setExtendJson(json);
        userMessage.setReadType(0);
        repo.save(userMessage);
    }

    @Async
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void saveOrUpdateMessage(UserMessage userMessage, UserMessageExtendDto dto, MessageUpsertTypeEnum type) {
        UserMessage dbMessage;
        if (type == MessageUpsertTypeEnum.COMMENT) {
            dbMessage = repo.findByUserIdAndSendUserIdAndVideoIdAndMessageTypeAndCommentId(
                    userMessage.getUserId(),
                    userMessage.getSendUserId(),
                    userMessage.getVideoId(),
                    userMessage.getMessageType(),
                    userMessage.getCommentId()
            );
        } else if (type == MessageUpsertTypeEnum.FOLLOW) {
            dbMessage = repo.findByUserIdAndSendUserIdAndMessageType(
                    userMessage.getUserId(),
                    userMessage.getSendUserId(),
                    userMessage.getMessageType()
            );
        } else {
            dbMessage = repo.findByUserIdAndSendUserIdAndVideoIdAndMessageType(
                    userMessage.getUserId(),
                    userMessage.getSendUserId(),
                    userMessage.getVideoId(),
                    userMessage.getMessageType()
            );
        }

        String extendJson = null;
        if (dto != null) {
            try {
                extendJson = mapper.writeValueAsString(dto);
            } catch (JsonProcessingException e) {
                log.error("Failed to convert UserMessageExtendDto to json string: {}", dto, e);
            }
        }

        UserMessage targetMessage = Objects.requireNonNullElse(dbMessage, userMessage);

        targetMessage.setExtendJson(extendJson);
        targetMessage.setCreatedAt(LocalDateTime.now());
        targetMessage.setReadType(0);

        if (type == MessageUpsertTypeEnum.COMMENT) {
            targetMessage.setCommentId(userMessage.getCommentId());
        }

        repo.save(targetMessage);
    }

    @Override
    public Integer getNoReadCount(Long userId) {
        return repo.countByUserIdAndReadType(userId, MessageReadTypeEnum.NO_READ.getType());
    }

    @Override
    public List<UserMessageCountVo> getMessageTypeWithNoReadCount(Long userId) {
        return repo.findMessageTypeWithNoReadCount(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void readAllMessage(String messageTypes, Long userId) {
        List<Integer> messageTypeList;
        if (messageTypes == null || messageTypes.isEmpty()) {
            messageTypeList = null;
        } else {
            messageTypeList = StringTools.parseDelimitedDistinctList(messageTypes, ",")
                    .stream()
                    .map(Integer::valueOf)
                    .toList();
        }
        repo.readAllMessage(messageTypeList, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMessage(Integer messageId, Long userId) {
        repo.deleteByMessageIdAndUserId(messageId, userId);
    }

    @Override
    public PaginationResultVo<UserMessage> getMessagePage(String messageTypes, Integer pageNo, Long userId) {
        List<Integer> messageTypeList;
        if (messageTypes == null || messageTypes.isEmpty()) {
            messageTypeList = null;
        } else {
            messageTypeList = StringTools.parseDelimitedDistinctList(messageTypes, ",")
                    .stream()
                    .map(Integer::valueOf)
                    .toList();
        }
        if (pageNo == null || pageNo < 1) pageNo = 1;
        Pageable pageable = PageRequest.of(pageNo - 1, Constants.PAGE_SIZE, Sort.by(Sort.Order.desc("messageId")));
        Page<UserMessage> page = repo.getMessagePage(messageTypeList, userId, pageable);
        return PaginationResultVo.fromPage(page);
    }
}
