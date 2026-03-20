package com.filesharing.repository;

import com.filesharing.entity.ProjectMember;
import com.filesharing.entity.CollaborationProject;
import com.filesharing.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {
    
    /**
     * 根据项目查找所有成员
     */
    List<ProjectMember> findByProject(CollaborationProject project);
    
    /**
     * 根据用户查找所有参与的项目
     */
    List<ProjectMember> findByUser(User user);
    
    /**
     * 查找用户在特定项目中的成员关系
     */
    Optional<ProjectMember> findByProjectAndUser(CollaborationProject project, User user);
    
    /**
     * 检查用户是否是项目成员
     */
    boolean existsByProjectAndUser(CollaborationProject project, User user);
    
    /**
     * 统计项目成员数量
     */
    long countByProject(CollaborationProject project);
    
    /**
     * 删除项目的所有成员关系
     */
    void deleteByProject(CollaborationProject project);
    
    /**
     * 查找具有特定角色的成员
     */
    List<ProjectMember> findByProjectAndRole(CollaborationProject project, ProjectMember.MemberRole role);
    
    /**
     * 查找项目管理员
     */
    default List<ProjectMember> findAdminsByProject(CollaborationProject project) {
        return findByProjectAndRole(project, ProjectMember.MemberRole.ADMIN);
    }
    
    /**
     * 查找活跃的项目成员
     */
    @Query("SELECT pm FROM ProjectMember pm WHERE pm.project = :project AND pm.status = 'ACTIVE'")
    List<ProjectMember> findActiveMembersByProject(CollaborationProject project);
}