package com.filesharing.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * FileCodeBox 风格的取件记录。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "pickup_code_records", indexes = {
        @Index(name = "idx_pickup_code", columnList = "code", unique = true),
        @Index(name = "idx_pickup_status_expire", columnList = "status, expire_at")
})
public class PickupCodeRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "share_type", nullable = false, length = 20)
    private ShareType shareType;

    @Lob
    @Column(name = "text_content", columnDefinition = "TEXT")
    private String textContent;

    @Column(name = "display_name", length = 255)
    private String displayName;

    @Column(name = "content_type", length = 120)
    private String contentType;

    @Column(name = "storage_path", length = 700)
    private String storagePath;

    @Enumerated(EnumType.STRING)
    @Column(name = "storage_mode", length = 30)
    private StorageMode storageMode = StorageMode.LOCAL;

    @Column(name = "cloud_config_id")
    private Long cloudConfigId;

    @Column(name = "size_bytes")
    private Long sizeBytes = 0L;

    @Column(name = "expire_at")
    private LocalDateTime expireAt;

    @Column(name = "expired_count")
    private Integer expiredCount = -1;

    @Column(name = "used_count")
    private Integer usedCount = 0;

    @Column(name = "created_ip", length = 64)
    private String createdIp;

    @Column(name = "creator_user_id")
    private Long creatorUserId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ShareStatus status = ShareStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum ShareType {
        FILE,
        TEXT
    }

    public enum ShareStatus {
        ACTIVE,
        EXPIRED,
        DISABLED
    }

    public enum StorageMode {
        LOCAL,
        CLOUD_DIRECT
    }
}
