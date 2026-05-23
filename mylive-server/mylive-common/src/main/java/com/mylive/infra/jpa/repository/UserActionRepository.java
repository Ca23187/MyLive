package com.mylive.infra.jpa.repository;

import com.mylive.infra.jpa.entity.po.UserAction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface UserActionRepository extends JpaRepository<UserAction, Long> {

    UserAction findByVideoIdAndCommentIdAndActionTypeAndUserId(String videoId, Long commentId, Integer actionType, Long userId);

    List<UserAction> findByVideoIdAndUserIdAndActionTypeIn(String videoId, Long userId, Collection<Integer> actionTypes);

    List<UserAction> findByCommentIdInAndUserIdAndActionTypeIn(Collection<Long> commentIds, Long userId, Collection<Integer> actionTypes);

    void deleteAllByVideoIdAndActionTypeIsNot(String videoId, Integer actionType);
}
