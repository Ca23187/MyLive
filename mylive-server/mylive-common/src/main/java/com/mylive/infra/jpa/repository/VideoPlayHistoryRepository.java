package com.mylive.infra.jpa.repository;

import com.mylive.infra.jpa.entity.po.VideoPlayHistory;
import com.mylive.infra.jpa.entity.po.id.VideoPlayHistoryId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface VideoPlayHistoryRepository extends JpaRepository<VideoPlayHistory, VideoPlayHistoryId> {
    @Query(
    value = "select v from VideoPlayHistory v where v.userId = :userId",
    countQuery = "select count(v) from VideoPlayHistory v where v.userId = :userId"
    )
    Page<VideoPlayHistory> getVideoPage(Long userId, Pageable pageable);

    void deleteAllByUserId(Long userId);
}
