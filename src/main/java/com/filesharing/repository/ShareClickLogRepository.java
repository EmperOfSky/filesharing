package com.filesharing.repository;

import com.filesharing.entity.ShareClickLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 分享点击日志仓库
 */
@Repository
public interface ShareClickLogRepository extends JpaRepository<ShareClickLog, Long> {

    long countByShare_Id(Long shareId);

    @Query("SELECT COUNT(DISTINCT l.visitorFingerprint) FROM ShareClickLog l WHERE l.share.id = :shareId")
    long countDistinctVisitorsByShareId(@Param("shareId") Long shareId);

    List<ShareClickLog> findByShare_IdOrderByAccessedAtDesc(Long shareId);
}
