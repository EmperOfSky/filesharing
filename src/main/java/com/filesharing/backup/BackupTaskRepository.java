package com.filesharing.backup;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BackupTaskRepository extends JpaRepository<BackupTaskEntity, String> {
}
