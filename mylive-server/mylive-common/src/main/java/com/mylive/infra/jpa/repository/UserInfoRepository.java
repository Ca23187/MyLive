package com.mylive.infra.jpa.repository;

import com.mylive.infra.jpa.entity.po.UserInfo;
import com.mylive.infra.jpa.entity.vo.VideoCommentVoItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface UserInfoRepository extends JpaRepository<UserInfo, Long> {
    UserInfo findByEmailOrNickname(String email, String nickname);

    UserInfo findByEmail(String email);

    @Modifying
    @Query("""
    update UserInfo u
    set u.currentCoinCount = u.currentCoinCount + :increment,
        u.totalCoinCount = u.totalCoinCount + :increment
    where u.userId = :userId
""")
    int increaseCoinCount(Long userId, int increment);

    @Modifying
    @Query("""
    update UserInfo u
    set u.currentCoinCount = u.currentCoinCount - :decrement
    where u.userId = :userId and u.currentCoinCount - :decrement >= 0
""")
    int decrCoinCount(Long userId, int decrement);

    UserInfo findByUserId(Long userId);

    @Query("update UserInfo u set u.theme = :theme where u.userId = :userId")
    @Modifying
    void updateThemeByUserId(Integer theme, Long userId);

    @Query("""
    select new com.mylive.infra.jpa.entity.vo.VideoCommentVoItem$MentionUserVo(
        u.userId,
        u.nickname
    )
    from UserInfo u
    where u.userId in :userIds
    """)
    List<VideoCommentVoItem.MentionUserVo> findMentionUsers(List<Long> userIds);

    @Query("select u.currentCoinCount from UserInfo u where u.userId = :userId")
    Integer findCoinCountByUserId(Long userId);

    @Query("update UserInfo u set u.status = 1 - u.status where u.userId = :userId")
    @Modifying
    void updateStatusByUserId(Long userId);

    @Query(value = """
    select u from UserInfo u
    where (:status is null or u.status = :status)
    and (:nicknameFuzzy is null
        or lower(u.nickname) like concat('%', :nicknameFuzzy, '%'))
    """, countQuery = """
    select count(u) from UserInfo u
    where (:status is null or u.status = :status)
    and (:nicknameFuzzy is null
        or lower(u.nickname) like concat('%', :nicknameFuzzy, '%'))
    """)
    Page<UserInfo> findUserInfoPage(String nicknameFuzzy, Integer status, Pageable pageable);
}
