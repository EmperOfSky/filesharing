package com.filesharing.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 文件夹标签关联实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "folder_tags")
public class FolderTag {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 关联的文件夹
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id", nullable = false)
    private Folder folder;
    
    /**
     * 关联的标签
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;
    
    /**
     * 添加标签的用户
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "added_by")
    private User addedBy;
    
    /**
     * 添加时间
     */
    @CreationTimestamp
    @Column(name = "added_at")
    private LocalDateTime addedAt;
}