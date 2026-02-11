package com.filesharing.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 搜索记录实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "search_records")
public class SearchRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 搜索关键词
     */
    @Column(name = "search_keyword", length = 200)
    private String searchKeyword;
    
    /**
     * 搜索用户
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    
    /**
     * 搜索类型：FILENAME, CONTENT, TAG, ALL
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "search_type", length = 20)
    private SearchType searchType;
    
    /**
     * 搜索结果数量
     */
    @Column(name = "result_count")
    private Integer resultCount = 0;
    
    /**
     * 搜索耗时（毫秒）
     */
    @Column(name = "search_duration")
    private Long searchDuration;
    
    /**
     * 是否包含过滤条件
     */
    @Column(name = "has_filters")
    private Boolean hasFilters = false;
    
    /**
     * 过滤条件JSON
     */
    @Column(name = "filter_conditions", length = 1000)
    private String filterConditions;
    
    /**
     * 搜索时间
     */
    @CreationTimestamp
    @Column(name = "search_time")
    private LocalDateTime searchTime;
    
    /**
     * 客户端IP
     */
    @Column(name = "client_ip", length = 45)
    private String clientIp;
    
    /**
     * 用户代理
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;
    
    /**
     * 搜索类型枚举
     */
    public enum SearchType {
        FILENAME,   // 文件名搜索
        CONTENT,    // 文件内容搜索
        TAG,        // 标签搜索
        ALL         // 全部搜索
    }
}