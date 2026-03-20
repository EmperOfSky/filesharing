package com.filesharing.repository;

import com.filesharing.entity.PickupCodeRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PickupCodeRecordRepository extends JpaRepository<PickupCodeRecord, Long> {

    Optional<PickupCodeRecord> findByCode(String code);

    boolean existsByCode(String code);

    List<PickupCodeRecord> findByStatus(PickupCodeRecord.ShareStatus status);

    @Query("SELECT r FROM PickupCodeRecord r " +
            "WHERE (:keyword IS NULL OR :keyword = '' " +
            "OR LOWER(r.code) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(COALESCE(r.displayName, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:status IS NULL OR r.status = :status) " +
            "AND (:shareType IS NULL OR r.shareType = :shareType)")
    Page<PickupCodeRecord> searchRecords(@Param("keyword") String keyword,
                                         @Param("status") PickupCodeRecord.ShareStatus status,
                                         @Param("shareType") PickupCodeRecord.ShareType shareType,
                                         Pageable pageable);

        @Query("SELECT r FROM PickupCodeRecord r " +
            "WHERE (:keyword IS NULL OR :keyword = '' " +
            "OR LOWER(r.code) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(COALESCE(r.displayName, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:status IS NULL OR r.status = :status) " +
            "AND (:shareType IS NULL OR r.shareType = :shareType) " +
            "ORDER BY r.createdAt DESC")
        List<PickupCodeRecord> searchRecordsForExport(@Param("keyword") String keyword,
                              @Param("status") PickupCodeRecord.ShareStatus status,
                              @Param("shareType") PickupCodeRecord.ShareType shareType);

    @Query("SELECT r FROM PickupCodeRecord r WHERE r.status = 'ACTIVE' AND r.expireAt IS NOT NULL AND r.expireAt < :now")
    List<PickupCodeRecord> findExpiredActive(@Param("now") LocalDateTime now);

    long countByCreatedAtGreaterThanEqual(LocalDateTime startTime);

    long countByCreatedAtBetween(LocalDateTime startTime, LocalDateTime endTime);

    @Query("SELECT COALESCE(SUM(r.sizeBytes), 0) FROM PickupCodeRecord r")
    Long sumAllSizeBytes();

    @Query("SELECT COALESCE(SUM(r.sizeBytes), 0) FROM PickupCodeRecord r WHERE r.createdAt >= :startTime")
    Long sumSizeByCreatedAtGreaterThanEqual(@Param("startTime") LocalDateTime startTime);

    @Query("SELECT COALESCE(SUM(r.sizeBytes), 0) FROM PickupCodeRecord r WHERE r.createdAt BETWEEN :startTime AND :endTime")
    Long sumSizeByCreatedAtBetween(@Param("startTime") LocalDateTime startTime,
                                   @Param("endTime") LocalDateTime endTime);
}
