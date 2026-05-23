package com.mylive.service.user;

import com.mylive.enums.MessageUpsertTypeEnum;
import com.mylive.infra.jpa.entity.dto.UserMessageExtendDto;
import com.mylive.infra.jpa.entity.po.UserMessage;
import com.mylive.infra.jpa.entity.vo.PaginationResultVo;
import com.mylive.infra.jpa.entity.vo.UserMessageCountVo;

import java.util.List;

public interface UserMessageService {
    void saveMessage(UserMessage userMessage, UserMessageExtendDto dto);

    void saveOrUpdateMessage(UserMessage userMessage, UserMessageExtendDto dto, MessageUpsertTypeEnum type);

    Integer getNoReadCount(Long userId);

    List<UserMessageCountVo> getMessageTypeWithNoReadCount(Long userId);

    void readAllMessage(String messageTypes, Long userId);

    void deleteMessage(Integer messageId, Long userId);

    PaginationResultVo<UserMessage> getMessagePage(String messageTypes, Integer pageNo, Long userId);
}
