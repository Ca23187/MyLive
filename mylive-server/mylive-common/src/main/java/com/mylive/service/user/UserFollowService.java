package com.mylive.service.user;

import com.mylive.infra.jpa.entity.dto.TokenInfo;
import com.mylive.infra.jpa.entity.vo.FollowOrFanListVo;
import com.mylive.infra.jpa.entity.vo.PaginationResultVo;

import java.util.List;

public interface UserFollowService {
    void followUser(TokenInfo tokenInfo, Long followUserId);

    void unFollow(Long userId, Long followUserId);

    PaginationResultVo<FollowOrFanListVo> findFollowPage(Integer pageNo, Long userId);

    PaginationResultVo<FollowOrFanListVo> findFanPage(Integer pageNo, Long userId);

    List<FollowOrFanListVo> searchMentionUserList(String keyword, Long userId);
}
