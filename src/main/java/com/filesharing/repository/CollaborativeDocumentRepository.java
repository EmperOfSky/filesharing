package com.filesharing.repository;

import com.filesharing.entity.CollaborativeDocument;
import com.filesharing.entity.CollaborationProject;
import com.filesharing.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CollaborativeDocumentRepository extends JpaRepository<CollaborativeDocument, Long> {
    
    /**
     * 根据项目查找所有文档
     */
    Page<CollaborativeDocument> findByProjectOrderByUpdatedAtDesc(CollaborationProject project, Pageable pageable);
    
    /**
     * 根据创建者查找文档
     */
    Page<CollaborativeDocument> findByCreatedByOrderByUpdatedAtDesc(User createdBy, Pageable pageable);
    
    /**
     * 根据文档名称搜索（模糊匹配）
     */
    Page<CollaborativeDocument> findByDocumentNameContainingIgnoreCaseOrderByUpdatedAtDesc(String documentName, Pageable pageable);
    
    /**
     * 根据文档类型查找
     */
    Page<CollaborativeDocument> findByDocumentTypeOrderByUpdatedAtDesc(CollaborativeDocument.DocumentType documentType, Pageable pageable);
    
    /**
     * 查找最近更新的文档
     */
    Page<CollaborativeDocument> findTop10ByOrderByUpdatedAtDesc(Pageable pageable);
    
    /**
     * 统计项目的文档数量
     */
    long countByProject(CollaborationProject project);
    
    /**
     * 删除项目的所有文档
     */
    void deleteByProject(CollaborationProject project);
    
    /**
     * 根据状态查找文档
     */
    Page<CollaborativeDocument> findByStatusOrderByUpdatedAtDesc(CollaborativeDocument.Status status, Pageable pageable);
    
    /**
     * 查找用户有权限访问的文档
     */
    @Query("SELECT DISTINCT cd FROM CollaborativeDocument cd " +
           "JOIN cd.project p " +
           "JOIN p.members pm " +
           "WHERE pm.user.id = :userId " +
           "ORDER BY cd.updatedAt DESC")
    Page<CollaborativeDocument> findDocumentsByUserPermission(Long userId, Pageable pageable);
    
    /**
     * 根据时间范围查找文档
     */
    @Query("SELECT cd FROM CollaborativeDocument cd WHERE cd.createdAt BETWEEN :startTime AND :endTime ORDER BY cd.createdAt DESC")
    List<CollaborativeDocument> findByCreatedAtBetween(LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 查找最近编辑的文档
     */
    @Query("SELECT cd FROM CollaborativeDocument cd WHERE cd.lastEditedAt >= :since ORDER BY cd.lastEditedAt DESC")
    Page<CollaborativeDocument> findRecentlyEditedSince(LocalDateTime since, Pageable pageable);
}