package com.filesharing.repository;

import com.filesharing.entity.AIModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AIModelRepository extends JpaRepository<AIModel, Long> {
    
    /**
     * 根据模型名称查找
     */
    Optional<AIModel> findByModelName(String modelName);
    
    /**
     * 根据提供商查找启用的模型
     */
    List<AIModel> findByProviderAndIsEnabledTrue(AIModel.Provider provider);
    
    /**
     * 根据模型类型查找启用的模型
     */
    List<AIModel> findByModelTypeAndIsEnabledTrue(AIModel.ModelType modelType);
    
    /**
     * 查找所有启用的模型
     */
    List<AIModel> findByIsEnabledTrue();
    
    /**
     * 检查模型名称是否存在
     */
    boolean existsByModelName(String modelName);
    
    /**
     * 根据提供商和模型类型查找
     */
    @Query("SELECT m FROM AIModel m WHERE m.provider = :provider AND m.modelType = :modelType AND m.isEnabled = true")
    List<AIModel> findByProviderAndModelType(AIModel.Provider provider, AIModel.ModelType modelType);
    
    /**
     * 统计各提供商的模型数量
     */
    @Query("SELECT m.provider, COUNT(m) FROM AIModel m WHERE m.isEnabled = true GROUP BY m.provider")
    List<Object[]> getModelCountByProvider();
    
    /**
     * 查找使用次数最多的模型
     */
    @Query("SELECT m FROM AIModel m WHERE m.isEnabled = true ORDER BY m.usageCount DESC")
    List<AIModel> findMostUsedModels();
}