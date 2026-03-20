package com.filesharing.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 分享短链接点击日志
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "share_click_logs")
public class ShareClickLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "share_id", nullable = false)
    private ShareRecord share;

    @Column(name = "share_key", nullable = false, length = 32)
    private String shareKey;

    @Column(name = "visitor_ip", length = 64)
    private String visitorIp;

    @Column(name = "visitor_address", length = 255)
    private String visitorAddress;

    @Column(name = "visitor_fingerprint", nullable = false, length = 64)
    private String visitorFingerprint;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "referer", length = 255)
    private String referer;

    @CreationTimestamp
    @Column(name = "accessed_at", nullable = false)
    private LocalDateTime accessedAt;
}
