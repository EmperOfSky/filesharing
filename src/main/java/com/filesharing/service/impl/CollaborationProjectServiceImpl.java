package com.filesharing.service.impl;

import com.filesharing.dto.*;
import com.filesharing.entity.CollaborationProject;
import com.filesharing.entity.ProjectMember;
import com.filesharing.entity.User;
import com.filesharing.exception.BusinessException;
import com.filesharing.repository.CollaborationProjectRepository;
import com.filesharing.repository.ProjectMemberRepository;
import com.filesharing.repository.UserRepository;
import com.filesharing.service.CollaborationProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 协作项目服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CollaborationProjectServiceImpl implements CollaborationProjectService {
    
    private final CollaborationProjectRepository projectRepository;
    private final ProjectMemberRepository memberRepository;
    private final UserRepository userRepository;
    
    @Override
    public ProjectResponseDTO createProject(ProjectCreateDTO projectCreateDTO, User currentUser) {
        // 创建项目
        CollaborationProject project = new CollaborationProject();
        project.setProjectName(projectCreateDTO.getProjectName());
        project.setDescription(projectCreateDTO.getDescription());
        // 将List<String>转换为逗号分隔的字符串
        if (projectCreateDTO.getTags() != null) {
            project.setTags(String.join(",", projectCreateDTO.getTags()));
        }
        project.setOwner(currentUser);
        project.setStatus(CollaborationProject.ProjectStatus.ACTIVE);
        project.setCreatedAt(LocalDateTime.now());
        project.setUpdatedAt(LocalDateTime.now());
        
        CollaborationProject savedProject = projectRepository.save(project);
        
        // 添加创建者为管理员成员
        ProjectMember member = new ProjectMember();
        member.setProject(savedProject);
        member.setUser(currentUser);
        member.setRole(ProjectMember.MemberRole.ADMIN);
        member.setStatus(ProjectMember.MemberStatus.ACTIVE);
        member.setInvitedBy(currentUser);
        member.setJoinedAt(LocalDateTime.now());
        memberRepository.save(member);
        
        log.info("创建协作项目: ID={}, 名称={}, 创建者={}", 
                savedProject.getId(), savedProject.getProjectName(), currentUser.getUsername());
        
        return convertToProjectResponse(savedProject, currentUser);
    }
    
    @Override
    @Transactional(readOnly = true)
    public ProjectResponseDTO getProjectById(Long projectId, User currentUser) {
        CollaborationProject project = projectRepository.findByIdWithMembers(projectId)
                .orElseThrow(() -> new BusinessException("项目不存在"));
        
        return convertToProjectResponse(project, currentUser);
    }
    
    @Override
    public ProjectResponseDTO updateProject(Long projectId, ProjectCreateDTO projectUpdateDTO, User currentUser) {
        CollaborationProject project = getProjectById(projectId);
        
        // 检查权限
        if (!hasProjectPermission(project, currentUser, ProjectMember.MemberRole.MEMBER)) {
            throw new BusinessException("无权限编辑此项目");
        }
        
        // 更新项目信息
        if (projectUpdateDTO.getProjectName() != null) {
            project.setProjectName(projectUpdateDTO.getProjectName());
        }
        if (projectUpdateDTO.getDescription() != null) {
            project.setDescription(projectUpdateDTO.getDescription());
        }
        if (projectUpdateDTO.getTags() != null) {
            project.setTags(String.join(",", projectUpdateDTO.getTags()));
        }
        
        project.setUpdatedAt(LocalDateTime.now());
        CollaborationProject updatedProject = projectRepository.save(project);
        
        log.info("更新协作项目: ID={}, 更新者={}", projectId, currentUser.getUsername());
        return convertToProjectResponse(updatedProject, currentUser);
    }
    
    @Override
    public void deleteProject(Long projectId, User currentUser) {
        CollaborationProject project = getProjectById(projectId);
        
        // 检查权限（只有项目所有者可以删除）
        if (!project.getOwner().getId().equals(currentUser.getId())) {
            throw new BusinessException("只有项目所有者可以删除项目");
        }
        
        project.setStatus(CollaborationProject.ProjectStatus.CLOSED);
        project.setUpdatedAt(LocalDateTime.now());
        projectRepository.save(project);
        
        log.info("删除协作项目: ID={}, 删除者={}", projectId, currentUser.getUsername());
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<ProjectResponseDTO> getUserProjects(User user, Pageable pageable) {
        List<CollaborationProject> projects = projectRepository.findProjectsByUser(user);
        return projectRepository.findAll(pageable)
                .map(project -> convertToProjectResponse(project, user));
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<ProjectResponseDTO> searchProjects(String keyword, Pageable pageable) {
        return projectRepository.findByProjectNameContainingIgnoreCase(keyword, pageable)
                .map(project -> convertToProjectResponse(project, null));
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<ProjectResponseDTO> getProjectsByTag(String tag, Pageable pageable) {
        return projectRepository.findByTag(tag, pageable)
                .map(project -> convertToProjectResponse(project, null));
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<ProjectResponseDTO> getActiveProjects(Pageable pageable) {
        return projectRepository.findActiveProjects(pageable)
                .map(project -> convertToProjectResponse(project, null));
    }
    
    @Override
    public MemberResponseDTO addProjectMember(Long projectId, MemberInviteDTO inviteDTO, User currentUser) {
        CollaborationProject project = getProjectById(projectId);
        
        // 检查邀请权限
        if (!hasProjectPermission(project, currentUser, ProjectMember.MemberRole.ADMIN)) {
            throw new BusinessException("无权限邀请成员");
        }
        
        // 查找被邀请用户
        User invitedUser = userRepository.findById(inviteDTO.getUserId())
                .orElseThrow(() -> new BusinessException("用户不存在"));
        
        // 检查是否已是成员
        if (memberRepository.existsByProjectAndUser(project, invitedUser)) {
            throw new BusinessException("用户已经是项目成员");
        }
        
        // 创建成员关系
        ProjectMember member = new ProjectMember();
        member.setProject(project);
        member.setUser(invitedUser);
        member.setRole(inviteDTO.getRole());
        member.setStatus(ProjectMember.MemberStatus.ACTIVE);
        member.setInvitedBy(currentUser);
        member.setJoinedAt(LocalDateTime.now());
        
        ProjectMember savedMember = memberRepository.save(member);
        
        log.info("添加项目成员: 项目ID={}, 用户={}, 角色={}", 
                projectId, invitedUser.getUsername(), inviteDTO.getRole());
        
        return convertToMemberResponse(savedMember);
    }
    
    @Override
    public void removeProjectMember(Long projectId, Long memberId, User currentUser) {
        CollaborationProject project = getProjectById(projectId);
        ProjectMember member = getProjectMemberById(memberId);
        
        // 检查权限
        if (!hasProjectPermission(project, currentUser, ProjectMember.MemberRole.ADMIN)) {
            throw new BusinessException("无权限移除成员");
        }
        
        // 不能移除自己（除非是项目所有者）
        if (member.getUser().getId().equals(currentUser.getId()) && 
            !project.getOwner().getId().equals(currentUser.getId())) {
            throw new BusinessException("不能移除自己");
        }
        
        memberRepository.delete(member);
        log.info("移除项目成员: 项目ID={}, 用户ID={}", projectId, memberId);
    }
    
    @Override
    public MemberResponseDTO updateMemberRole(Long projectId, Long memberId, 
                                            ProjectMember.MemberRole newRole, String permissions, User currentUser) {
        CollaborationProject project = getProjectById(projectId);
        ProjectMember member = getProjectMemberById(memberId);
        
        // 检查权限
        if (!hasProjectPermission(project, currentUser, ProjectMember.MemberRole.ADMIN)) {
            throw new BusinessException("无权限更新成员角色");
        }
        
        // 不能修改项目所有者的角色
        if (member.getUser().getId().equals(project.getOwner().getId())) {
            throw new BusinessException("不能修改项目所有者的角色");
        }
        
        member.setRole(newRole);
        ProjectMember updatedMember = memberRepository.save(member);
        
        log.info("更新成员角色: 项目ID={}, 用户={}, 新角色={}", 
                projectId, member.getUser().getUsername(), newRole.name());
        
        return convertToMemberResponse(updatedMember);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<MemberResponseDTO> getProjectMembers(Long projectId, User currentUser) {
        CollaborationProject project = getProjectById(projectId);
        return memberRepository.findByProject(project)
                .stream()
                .map(this::convertToMemberResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean hasProjectAccess(Long projectId, User user) {
        try {
            CollaborationProject project = getProjectById(projectId);
            return hasProjectPermission(project, user, ProjectMember.MemberRole.VIEWER);
        } catch (BusinessException e) {
            return false;
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public ProjectStatsDTO getProjectStats(Long projectId) {
        CollaborationProject project = getProjectById(projectId);
        
        ProjectStatsDTO stats = new ProjectStatsDTO();
        stats.setProjectId(projectId);
        stats.setTotalMembers(memberRepository.countByProject(project));
        stats.setTotalDocuments(project.getFileCount() != null ? project.getFileCount().longValue() : 0L);
        stats.setTotalComments(0L); // 需要根据实际评论表来计算
        
        return stats;
    }
    
    // ==================== 私有方法 ====================
    
    private CollaborationProject getProjectById(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException("项目不存在"));
    }
    
    private ProjectMember getProjectMemberById(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException("成员不存在"));
    }
    
    private boolean hasRolePriority(ProjectMember.MemberRole userRole, ProjectMember.MemberRole requiredRole) {
        // 定义角色优先级：OWNER > ADMIN > MEMBER > VIEWER
        int userPriority = getRolePriority(userRole);
        int requiredPriority = getRolePriority(requiredRole);
        return userPriority >= requiredPriority;
    }
    
    private int getRolePriority(ProjectMember.MemberRole role) {
        switch (role) {
            case OWNER: return 4;
            case ADMIN: return 3;
            case MEMBER: return 2;
            case VIEWER: return 1;
            default: return 0;
        }
    }
    
    private boolean hasProjectPermission(CollaborationProject project, User user, 
                                       ProjectMember.MemberRole requiredRole) {
        // 项目所有者拥有最高权限
        if (project.getOwner().getId().equals(user.getId())) {
            return true;
        }
        
        // 检查成员权限
        return memberRepository.findByProjectAndUser(project, user)
                .map(member -> hasRolePriority(member.getRole(), requiredRole))
                .orElse(false);
    }
    
    private ProjectResponseDTO convertToProjectResponse(CollaborationProject project, User currentUser) {
        ProjectResponseDTO response = new ProjectResponseDTO();
        response.setId(project.getId());
        response.setProjectName(project.getProjectName());
        response.setDescription(project.getDescription());
        // 处理标签：将逗号分隔的字符串转换为List
        if (project.getTags() != null && !project.getTags().isEmpty()) {
            response.setTags(Arrays.asList(project.getTags().split(",")));
        } else {
            response.setTags(new ArrayList<>());
        }
        response.setStatus(project.getStatus());
        
        // 设置所有者信息
        ProjectResponseDTO.UserInfoDTO ownerResponse = new ProjectResponseDTO.UserInfoDTO();
        ownerResponse.setId(project.getOwner().getId());
        ownerResponse.setUsername(project.getOwner().getUsername());
        ownerResponse.setEmail(project.getOwner().getEmail());
        response.setOwner(ownerResponse);
        
        // 设置成员数量
        response.setMemberCount(memberRepository.countByProject(project));
        
        response.setCreatedAt(project.getCreatedAt());
        response.setUpdatedAt(project.getUpdatedAt());
        
        // 注意：ProjectResponseDTO中没有setCurrentUserIsOwner和setCurrentUserIsMember方法
        // 这些信息可以通过其他方式传递给前端
        
        return response;
    }
    
    private MemberResponseDTO convertToMemberResponse(ProjectMember member) {
        MemberResponseDTO response = new MemberResponseDTO();
        response.setId(member.getId());
        
        // 设置用户信息
        MemberResponseDTO.UserInfoDTO userInfo = new MemberResponseDTO.UserInfoDTO();
        userInfo.setId(member.getUser().getId());
        userInfo.setUsername(member.getUser().getUsername());
        userInfo.setEmail(member.getUser().getEmail());
        userInfo.setAvatar(member.getUser().getAvatar());
        response.setUser(userInfo);
        
        response.setRole(member.getRole());
        response.setPermissions(member.getPermissions());
        response.setJoinedAt(member.getJoinedAt());
        response.setLastActiveAt(member.getLastActivity());
        
        return response;
    }
}