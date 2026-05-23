package com.mylive.infra.jpa.repository;

import com.mylive.infra.jpa.entity.po.UserMessage;
import com.mylive.infra.jpa.entity.vo.UserMessageCountVo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface UserMessageRepository extends JpaRepository<UserMessage, Long> {

    UserMessage findByUserIdAndSendUserIdAndVideoIdAndMessageType(Long userId, Long sendUserId, String videoId, Integer messageType);

    UserMessage findByUserIdAndSendUserIdAndVideoIdAndMessageTypeAndCommentId(Long userId, Long sendUserId, String videoId, Integer messageType, Long commentId);

    UserMessage findByUserIdAndSendUserIdAndMessageType(Long userId, Long sendUserId, Integer messageType);

    Integer countByUserIdAndReadType(Long userId, Integer readType);

    @Query("""
    select new com.mylive.infra.jpa.entity.vo.UserMessageCountVo(
        u.messageType, count(u)
    ) from UserMessage u
        where u.userId = :userId and u.readType = 0
            group by u.messageType
    """)
    List<UserMessageCountVo> findMessageTypeWithNoReadCount(Long userId);

    @Modifying
    @Query("""
    update UserMessage u set u.readType = 1
        where u.userId = :userId
            and (u.messageType is null or u.messageType in :messageTypeList)
            and u.readType = 0
    """)
    void readAllMessage(List<Integer> messageTypeList, Long userId);

    void deleteByMessageIdAndUserId(Integer messageId, Long userId);

    @Query(value = """
    select u from UserMessage u
        where u.userId = :userId
            and (u.messageType is null or u.messageType in :messageTypeList)
    """, countQuery = """
    select count(u) from UserMessage u
        where u.userId = :userId
            and (u.messageType is null or u.messageType in :messageTypeList)
    """)
    Page<UserMessage> getMessagePage(List<Integer> messageTypeList, Long userId, Pageable pageable);
}
