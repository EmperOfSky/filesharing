package com.filesharing.service;

import com.filesharing.dto.*;
import com.filesharing.entity.ProjectMember;
import com.filesharing.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 协作项目服务接口
 */
public interface CollaborationProjectService {
    
    /**
     * 创建协作项目
     */
    ProjectResponseDTO createProject(ProjectCreateDTO projectCreateDTO, User currentUser);
    
    /**
     * 获取项目详情
     */
    ProjectResponseDTO getProjectById(Long projectId, User currentUser);
    
    /**
     * 更新项目信息
     */
    ProjectResponseDTO updateProject(Long projectId, ProjectCreateDTO projectUpdateDTO, User currentUser);
    
    /**
     * 删除项目
     */
    void deleteProject(Long projectId, User currentUser);
    
    /**
     * 获取用户的所有项目
     */
    Page<ProjectResponseDTO> getUserProjects(User user, Pageable pageable);
    
    /**
     * 搜索项目
     */
    Page<ProjectResponseDTO> searchProjects(String keyword, Pageable pageable);
    
    /**
     * 根据标签筛选项目
     */
    Page<ProjectResponseDTO> getProjectsByTag(String tag, Pageable pageable);
    
    /**
     * 获取活跃项目列表
     */
    Page<ProjectResponseDTO> getActiveProjects(Pageable pageable);
    
    /**
     * 添加项目成员
     */
    MemberResponseDTO addProjectMember(Long projectId, MemberInviteDTO inviteDTO, User currentUser);
    
    /**
     * 移除项目成员
     */
    void removeProjectMember(Long projectId, Long memberId, User currentUser);
    
    /**
     * 更新成员角色和权限
     */
    MemberResponseDTO updateMemberRole(Long projectId, Long memberId, 
                                     ProjectMember.MemberRole newRole, String permissions, User currentUser);
    
    /**
     * 获取项目成员列表
     */
    List<MemberResponseDTO> getProjectMembers(Long projectId, User currentUser);
    
    /**
     * 检查用户是否有项目访问权限
     */
    boolean hasProjectAccess(Long projectId, User user);
    
    /**
     * 获取项目统计信息
     */
    ProjectStatsDTO getProjectStats(Long projectId);
}