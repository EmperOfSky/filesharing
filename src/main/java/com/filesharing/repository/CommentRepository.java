package com.filesharing.repository;

import com.filesharing.entity.Comment;
import com.filesharing.entity.User;
import com.filesharing.entity.FileEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    
    /**
     * 根据文件查找所有评论
     */
    Page<Comment> findByFileOrderByCreatedAtDesc(FileEntity file, Pageable pageable);
    
    /**
     * 根据用户查找所有评论
     */
    Page<Comment> findByAuthorOrderByCreatedAtDesc(User author, Pageable pageable);
    
    /**
     * 统计文件的评论数量
     */
    long countByFile(FileEntity file);
    
    /**
     * 删除文件的所有评论
     */
    void deleteByFile(FileEntity file);
    
    /**
     * 查找最近的评论
     */
    Page<Comment> findTop10ByOrderByCreatedAtDesc(Pageable pageable);
    
    /**
     * 根据时间范围查找评论
     */
    @Query("SELECT c FROM Comment c WHERE c.createdAt BETWEEN :startTime AND :endTime ORDER BY c.createdAt DESC")
    List<Comment> findByCreatedAtBetween(LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 查找回复评论
     */
    List<Comment> findByParentId(Long parentId);
    
    /**
     * 检查是否存在回复
     */
    boolean existsByParentId(Long parentId);
}