package com.filesharing.repository;

import com.filesharing.entity.FileTag;
import com.filesharing.entity.FileEntity;
import com.filesharing.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 文件标签Repository接口
 */
@Repository
public interface FileTagRepository extends JpaRepository<FileTag, Long> {
    
    /**
     * 根据文件和标签查找关联记录
     */
    Optional<FileTag> findByFileAndTag(FileEntity file, Tag tag);
    
    /**
     * 根据文件查找所有标签
     */
    @Query("SELECT ft.tag FROM FileTag ft WHERE ft.file = :file")
    List<Tag> findTagsByFile(@Param("file") FileEntity file);
    
    /**
     * 根据标签查找所有文件
     */
    @Query("SELECT ft.file FROM FileTag ft WHERE ft.tag = :tag")
    List<FileEntity> findFilesByTag(@Param("tag") Tag tag);
    
    /**
     * 根据文件ID查找标签
     */
    @Query("SELECT ft.tag FROM FileTag ft WHERE ft.file.id = :fileId")
    List<Tag> findTagsByFileId(@Param("fileId") Long fileId);
    
    /**
     * 检查文件是否已关联指定标签
     */
    @Query("SELECT CASE WHEN COUNT(ft) > 0 THEN true ELSE false END FROM FileTag ft " +
           "WHERE ft.file = :file AND ft.tag = :tag")
    boolean existsByFileAndTag(@Param("file") FileEntity file, @Param("tag") Tag tag);
    
    /**
     * 根据文件删除所有标签关联
     */
    void deleteByFile(FileEntity file);
    
    /**
     * 根据标签删除所有关联
     */
    void deleteByTag(Tag tag);
    
    /**
     * 统计标签的文件使用次数
     */
    @Query("SELECT COUNT(ft) FROM FileTag ft WHERE ft.tag = :tag")
    Long countByTag(@Param("tag") Tag tag);
    
    /**
     * 查找具有相同标签的文件
     */
    @Query("SELECT DISTINCT f FROM FileEntity f JOIN f.fileTags ft " +
           "WHERE ft.tag IN :tags AND f.id != :excludeFileId")
    List<FileEntity> findFilesWithSameTags(@Param("tags") List<Tag> tags, 
                                          @Param("excludeFileId") Long excludeFileId);
    
    /**
     * 获取文件的标签名称列表
     */
    @Query("SELECT ft.tag.tagName FROM FileTag ft WHERE ft.file = :file")
    List<String> findTagNamesByFile(@Param("file") FileEntity file);
}