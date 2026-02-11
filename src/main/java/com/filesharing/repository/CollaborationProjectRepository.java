package com.filesharing.repository;

import com.filesharing.entity.CollaborationProject;
import com.filesharing.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 协作项目Repository接口
 */
@Repository
public interface CollaborationProjectRepository extends JpaRepository<CollaborationProject, Long> {
    
    /**
     * 根据项目名称查找项目（模糊查询）
     */
    Page<CollaborationProject> findByProjectNameContainingIgnoreCase(String projectName, Pageable pageable);
    
    /**
     * 根据拥有者查找项目
     */
    List<CollaborationProject> findByOwner(User owner);
    
    /**
     * 根据拥有者和状态查找项目
     */
    List<CollaborationProject> findByOwnerAndStatus(User owner, CollaborationProject.ProjectStatus status);
    
    /**
     * 根据状态查找项目
     */
    Page<CollaborationProject> findByStatus(CollaborationProject.ProjectStatus status, Pageable pageable);
    
    /**
     * 查找用户参与的所有项目（包括作为成员的项目）
     */
    @Query("SELECT DISTINCT cp FROM CollaborationProject cp " +
           "LEFT JOIN cp.members pm " +
           "WHERE cp.owner = :user OR pm.user = :user")
    List<CollaborationProject> findProjectsByUser(@Param("user") User user);
    
    /**
     * 查找活跃的项目
     */
    @Query("SELECT cp FROM CollaborationProject cp WHERE cp.status = 'ACTIVE'")
    Page<CollaborationProject> findActiveProjects(Pageable pageable);
    
    /**
     * 根据标签查找项目
     */
    @Query("SELECT cp FROM CollaborationProject cp WHERE cp.tags LIKE %:tag%")
    Page<CollaborationProject> findByTag(@Param("tag") String tag, Pageable pageable);
    
    /**
     * 查找项目及其成员信息
     */
    @Query("SELECT cp FROM CollaborationProject cp LEFT JOIN FETCH cp.members WHERE cp.id = :id")
    Optional<CollaborationProject> findByIdWithMembers(@Param("id") Long id);
    
    /**
     * 统计用户拥有的项目数量
     */
    long countByOwner(User owner);
    
    /**
     * 统计特定状态的项目数量
     */
    long countByStatus(CollaborationProject.ProjectStatus status);
}