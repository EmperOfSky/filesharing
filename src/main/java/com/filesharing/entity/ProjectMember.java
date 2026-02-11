package com.filesharing.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 项目成员实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "project_members")
public class ProjectMember {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 关联的项目
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private CollaborationProject project;
    
    /**
     * 成员用户
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    /**
     * 成员角色：OWNER, ADMIN, MEMBER, VIEWER
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private MemberRole role = MemberRole.MEMBER;
    
    /**
     * 成员状态：ACTIVE, INACTIVE, LEFT
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private MemberStatus status = MemberStatus.ACTIVE;
    
    /**
     * 邀请状态：PENDING, ACCEPTED, REJECTED
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "invite_status", length = 20)
    private InviteStatus inviteStatus = InviteStatus.ACCEPTED;
    
    /**
     * 邀请人
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invited_by")
    private User invitedBy;
    
    /**
     * 加入时间
     */
    @CreationTimestamp
    @Column(name = "joined_at")
    private LocalDateTime joinedAt;
    
    /**
     * 最后活动时间
     */
    @Column(name = "last_activity")
    private LocalDateTime lastActivity;
    
    /**
     * 权限设置（JSON格式）
     */
    @Column(name = "permissions", columnDefinition = "TEXT")
    private String permissions;
    
    /**
     * 成员角色枚举
     */
    public enum MemberRole {
        OWNER,      // 所有者
        ADMIN,      // 管理员
        MEMBER,     // 普通成员
        VIEWER      // 只读成员
    }
    
    /**
     * 成员状态枚举
     */
    public enum MemberStatus {
        ACTIVE,     // 活跃
        INACTIVE,   // 不活跃
        LEFT        // 已离开
    }
    
    /**
     * 邀请状态枚举
     */
    public enum InviteStatus {
        PENDING,    // 待接受
        ACCEPTED,   // 已接受
        REJECTED    // 已拒绝
    }
}