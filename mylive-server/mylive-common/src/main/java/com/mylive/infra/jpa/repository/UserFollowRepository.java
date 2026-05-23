package com.mylive.infra.jpa.repository;

import com.mylive.infra.jpa.entity.dto.FollowStats;
import com.mylive.infra.jpa.entity.po.UserFollow;
import com.mylive.infra.jpa.entity.po.id.UserFollowId;
import com.mylive.infra.jpa.entity.vo.FollowOrFanListVo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface UserFollowRepository extends JpaRepository<UserFollow, UserFollowId> {

    @Query("""
    select new com.mylive.infra.jpa.entity.dto.FollowStats(
        coalesce(sum(case when f.userId = :userId then 1 else 0 end), 0),
        coalesce(sum(case when f.followUserId = :userId then 1 else 0 end), 0)
    )
    from UserFollow f
    where f.userId = :userId or f.followUserId = :userId
    """)
    FollowStats countFollowStats(Long userId);

    @Query(value = """
    select new com.mylive.infra.jpa.entity.vo.FollowOrFanListVo(
        u.followUserId,
        u2.nickname,
        u2.avatar,
        u2.profile,
        case
            when exists (
                select 1 from UserFollow f
                where f.userId = u.followUserId
                and f.followUserId = :currentUserId
            )
            then 1 else 0
        end
    )
    from UserFollow u
    join UserInfo u2 on u.followUserId = u2.userId
    where u.userId = :currentUserId
    """, countQuery = """
    select count(u)
    from UserFollow u
    where u.userId = :currentUserId
    """
    )
    Page<FollowOrFanListVo> getFollowPage(Long currentUserId, Pageable pageable);

    @Query(value = """
    select new com.mylive.infra.jpa.entity.vo.FollowOrFanListVo(
        u.userId,
        u1.nickname,
        u1.avatar,
        u1.profile,
        case
            when exists (
                select 1 from UserFollow f
                where f.userId = :currentUserId
                and f.followUserId = u.userId
            )
            then 1 else 0
        end
    )
    from UserFollow u
    join UserInfo u1 on u.userId = u1.userId
    where u.followUserId = :currentUserId
    """, countQuery = """
    select count(u)
    from UserFollow u
    where u.followUserId = :currentUserId
    """)
    Page<FollowOrFanListVo> getFanPage(Long currentUserId, Pageable pageable);

    @Query(value = """
    select new com.mylive.infra.jpa.entity.vo.FollowOrFanListVo(
        u.followUserId,
        u2.nickname,
        u2.avatar,
        null,
        null
    )
    from UserFollow u
    join UserInfo u2 on u.followUserId = u2.userId
    where u.userId = :currentUserId
    and (
        :keyword is null
        or lower(u2.nickname) like concat('%', :keyword, '%')
    )
    """)
    List<FollowOrFanListVo> searchMentionUserList(Long currentUserId, String keyword, Pageable pageable);

    long countByFollowUserId(Long followUserId);

    int deleteByUserIdAndFollowUserId(Long userId, Long followUserId);
}
