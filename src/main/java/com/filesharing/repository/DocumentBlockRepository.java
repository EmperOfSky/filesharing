package com.filesharing.repository;

import com.filesharing.entity.DocumentBlock;
import com.filesharing.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentBlockRepository extends JpaRepository<DocumentBlock, Long> {

    List<DocumentBlock> findByDocumentIdOrderByOrderIndexAsc(Long documentId);

    long countByDocumentId(Long documentId);

    Optional<DocumentBlock> findByIdAndDocumentId(Long id, Long documentId);

    @Modifying
    @Query("UPDATE DocumentBlock b SET b.orderIndex = b.orderIndex + 1 " +
            "WHERE b.document.id = :documentId AND b.orderIndex > :afterOrderIndex")
    int shiftOrderIndexAfter(
            @Param("documentId") Long documentId,
            @Param("afterOrderIndex") Integer afterOrderIndex);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE DocumentBlock b SET b.lockedBy = :user, b.lockedAt = :lockedAt " +
            "WHERE b.document.id = :documentId AND b.id = :blockId " +
            "AND (b.lockedBy IS NULL OR b.lockedBy.id = :currentUserId)")
    int lockBlock(
            @Param("documentId") Long documentId,
            @Param("blockId") Long blockId,
            @Param("user") User user,
            @Param("lockedAt") LocalDateTime lockedAt,
            @Param("currentUserId") Long currentUserId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE DocumentBlock b SET b.lockedBy = null, b.lockedAt = null " +
            "WHERE b.document.id = :documentId AND b.id = :blockId " +
            "AND (b.lockedBy IS NULL OR b.lockedBy.id = :currentUserId)")
    int unlockBlock(
            @Param("documentId") Long documentId,
            @Param("blockId") Long blockId,
            @Param("currentUserId") Long currentUserId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE DocumentBlock b SET b.lockedBy = null, b.lockedAt = null " +
            "WHERE b.document.id = :documentId AND b.id = :blockId")
    int forceUnlockBlock(
            @Param("documentId") Long documentId,
            @Param("blockId") Long blockId);

    @Modifying
    @Query("UPDATE DocumentBlock b SET b.lockedBy = null, b.lockedAt = null " +
            "WHERE b.document.id = :documentId AND b.lockedBy.id = :userId")
    int clearLocksByDocumentAndUser(@Param("documentId") Long documentId, @Param("userId") Long userId);
}
