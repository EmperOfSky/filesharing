package com.filesharing.repository;

import com.filesharing.entity.Folder;
import com.filesharing.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 文件夹Repository接口
 */
@Repository
public interface FolderRepository extends JpaRepository<Folder, Long> {
    
    /**
     * 根据所有者查找根文件夹（没有父文件夹的文件夹）
     */
    @Query("SELECT f FROM Folder f WHERE f.owner = :owner AND f.parent IS NULL")
    List<Folder> findRootFoldersByOwner(@Param("owner") User owner);
    
    /**
     * 根据所有者和父文件夹查找子文件夹
     */
    List<Folder> findByOwnerAndParent(User owner, Folder parent);
    
    /**
     * 根据所有者查找所有文件夹
     */
    List<Folder> findByOwner(User owner);
    
    /**
     * 根据所有者和文件夹名称查找
     */
    @Query("SELECT f FROM Folder f WHERE f.owner = :owner AND f.name = :name AND f.parent = :parent")
    Optional<Folder> findByOwnerAndNameAndParent(@Param("owner") User owner, 
                                                @Param("name") String name, 
                                                @Param("parent") Folder parent);
    
    /**
     * 查找公开文件夹
     */
    @Query("SELECT f FROM Folder f WHERE f.isPublic = true")
    List<Folder> findPublicFolders();
    
    /**
     * 根据路径查找文件夹
     */
    @Query("SELECT f FROM Folder f WHERE f.folderPath = :path AND f.owner = :owner")
    Optional<Folder> findByFolderPathAndOwner(@Param("path") String path, @Param("owner") User owner);
    
    /**
     * 统计用户文件夹数量
     */
    @Query("SELECT COUNT(f) FROM Folder f WHERE f.owner = :owner")
    Long countByOwner(@Param("owner") User owner);
    
    /**
     * 查找包含特定文件的文件夹
     */
    @Query("SELECT DISTINCT f.folder FROM FileEntity f WHERE f.id = :fileId")
    List<Folder> findFoldersContainingFile(@Param("fileId") Long fileId);
}