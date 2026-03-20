package com.filesharing.backup;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "backup_task")
@Data
public class BackupTaskEntity {
    @Id
    @Column(length = 64)
    private String taskId;

    private String backupName;
    private String backupType;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Boolean success;
    private String errorMessage;
    private String dbBackupPath;
    private String filesBackupPath;
    private String metadataPath;
    private Long backedUpFileCount;
}
