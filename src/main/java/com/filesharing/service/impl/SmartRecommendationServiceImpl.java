package com.filesharing.service.impl;

import com.filesharing.entity.*;
import com.filesharing.exception.BusinessException;
import com.filesharing.repository.*;
import com.filesharing.service.SmartRecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 智能推荐服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SmartRecommendationServiceImpl implements SmartRecommendationService {
    
    private final SmartRecommendationRepository smartRecommendationRepository;
    private final FileRepository fileRepository;
    private final FolderRepository folderRepository;
    private final UserRepository userRepository;
    private final TagRepository tagRepository;
    private final FileTagRepository fileTagRepository;
    private final UserBehaviorStatisticsRepository userBehaviorStatisticsRepository;
    
    // 推荐算法参数
    private static final double SIMILARITY_THRESHOLD = 0.6;
    private static final int MAX_RECOMMENDATIONS = 20;
    private static final int RECOMMENDATION_EXPIRE_DAYS = 7;
    private static final double DECAY_FACTOR = 0.95;
    
    @Override
    @Transactional
    public List<SmartRecommendation> generatePersonalizedRecommendations(User user) {
        try {
            List<SmartRecommendation> recommendations = new ArrayList<>();
            
            // 1. 基于用户行为的协同过滤推荐
            recommendations.addAll(generateCollaborativeFilteringRecommendations(user));
            
            // 2. 基于内容的相似性推荐
            recommendations.addAll(generateContentBasedRecommendations(user));
            
            // 3. 基于热门趋势的推荐
            recommendations.addAll(generateTrendingRecommendations(user));
            
            // 4. 基于标签关联的推荐
            recommendations.addAll(generateTagBasedRecommendations(user));
            
            // 5. 基于时间上下文的推荐
            recommendations.addAll(generateContextualRecommendations(user));
            
            // 去重并排序
            recommendations = deduplicateAndSort(recommendations);
            
            // 限制推荐数量
            if (recommendations.size() > MAX_RECOMMENDATIONS) {
                recommendations = recommendations.subList(0, MAX_RECOMMENDATIONS);
            }
            
            // 保存推荐记录
            recommendations.forEach(rec -> {
                rec.setUser(user);
                rec.setExpireAt(LocalDateTime.now().plusDays(RECOMMENDATION_EXPIRE_DAYS));
            });
            
            List<SmartRecommendation> savedRecommendations = smartRecommendationRepository.saveAll(recommendations);
            
            log.info("为用户 {} 生成个性化推荐 {} 条", user.getUsername(), savedRecommendations.size());
            return savedRecommendations;
            
        } catch (Exception e) {
            log.error("生成个性化推荐失败: 用户={}", user.getUsername(), e);
            throw new BusinessException("推荐服务暂时不可用");
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<SmartRecommendation> getUserRecommendations(User user, int page, int size) {
        return smartRecommendationRepository.findByUserAndIsViewedFalseOrderByCreatedAtDesc(
            user, PageRequest.of(page, size));
    }
    
    @Override
    @Transactional
    public void markRecommendationViewed(Long recommendationId, User user) {
        SmartRecommendation recommendation = smartRecommendationRepository.findById(recommendationId)
            .orElseThrow(() -> new BusinessException("推荐记录不存在"));
            
        if (!recommendation.getUser().getId().equals(user.getId())) {
            throw new BusinessException("无权操作此推荐记录");
        }
        
        recommendation.setIsViewed(true);
        recommendation.setViewedAt(LocalDateTime.now());
        smartRecommendationRepository.save(recommendation);
        
        log.debug("标记推荐已查看: 用户={}, 推荐ID={}", user.getUsername(), recommendationId);
    }
    
    @Override
    @Transactional
    public void markRecommendationAdopted(Long recommendationId, User user) {
        SmartRecommendation recommendation = smartRecommendationRepository.findById(recommendationId)
            .orElseThrow(() -> new BusinessException("推荐记录不存在"));
            
        if (!recommendation.getUser().getId().equals(user.getId())) {
            throw new BusinessException("无权操作此推荐记录");
        }
        
        recommendation.setIsAdopted(true);
        recommendation.setAdoptedAt(LocalDateTime.now());
        smartRecommendationRepository.save(recommendation);
        
        log.info("标记推荐已采纳: 用户={}, 推荐ID={}, 类型={}", 
            user.getUsername(), recommendationId, recommendation.getRecommendationType());
    }
    
    @Override
    @Transactional
    public void cleanupExpiredRecommendations() {
        LocalDateTime expiredDate = LocalDateTime.now().minusDays(RECOMMENDATION_EXPIRE_DAYS);
        int deletedCount = smartRecommendationRepository.deleteByExpireAtBefore(expiredDate);
        log.info("清理过期推荐记录: 删除数量={}", deletedCount);
    }
    
    @Override
    @Transactional(readOnly = true)
    public RecommendationAnalytics getRecommendationAnalytics(User user) {
        RecommendationAnalytics analytics = new RecommendationAnalytics();
        
        // 获取用户的所有推荐记录
        List<SmartRecommendation> recommendations = smartRecommendationRepository.findByUser(user);
        
        analytics.setTotalRecommendations((long) recommendations.size());
        analytics.setViewedRecommendations(recommendations.stream()
            .filter(SmartRecommendation::getIsViewed)
            .count());
        analytics.setAdoptedRecommendations(recommendations.stream()
            .filter(SmartRecommendation::getIsAdopted)
            .count());
        
        // 计算查看率和采纳率
        if (analytics.getTotalRecommendations() > 0) {
            analytics.setViewRate((double) analytics.getViewedRecommendations() / 
                analytics.getTotalRecommendations() * 100);
            analytics.setAdoptionRate(analytics.getViewedRecommendations() > 0 ? 
                (double) analytics.getAdoptedRecommendations() / analytics.getViewedRecommendations() * 100 : 0.0);
        }
        
        // 按类型统计
        Map<SmartRecommendation.RecommendationType, Long> typeStats = recommendations.stream()
            .collect(Collectors.groupingBy(SmartRecommendation::getRecommendationType, Collectors.counting()));
        analytics.setTypeDistribution(typeStats);
        
        // 按来源统计
        Map<SmartRecommendation.SourceType, Long> sourceStats = recommendations.stream()
            .collect(Collectors.groupingBy(SmartRecommendation::getSourceType, Collectors.counting()));
        analytics.setSourceDistribution(sourceStats);
        
        return analytics;
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<SmartRecommendation> getSimilarItemsRecommendation(Long itemId, 
        SmartRecommendation.RecommendationType type, User user) {
        
        List<SmartRecommendation> recommendations = new ArrayList<>();
        
        switch (type) {
            case FILE:
                recommendations.addAll(getSimilarFiles(itemId, user));
                break;
            case FOLDER:
                recommendations.addAll(getSimilarFolders(itemId, user));
                break;
            case TAG:
                recommendations.addAll(getRelatedTags(itemId, user));
                break;
            default:
                throw new BusinessException("不支持的推荐类型: " + type);
        }
        
        return recommendations;
    }
    
    // ==================== 私有方法 ====================
    
    /**
     * 生成协同过滤推荐（基于用户行为相似性）
     */
    private List<SmartRecommendation> generateCollaborativeFilteringRecommendations(User targetUser) {
        List<SmartRecommendation> recommendations = new ArrayList<>();
        
        try {
            // 获取行为相似的用户
            List<User> similarUsers = findBehaviorSimilarUsers(targetUser, 10);
            
            // 收集相似用户喜欢的内容
            Set<Long> recommendedItemIds = new HashSet<>();
            
            for (User similarUser : similarUsers) {
                // 获取相似用户最近互动的文件
                List<FileEntity> similarUserFiles = fileRepository.findByUploader(similarUser, 
                    PageRequest.of(0, 20)).getContent();
                
                for (FileEntity file : similarUserFiles) {
                    if (!recommendedItemIds.contains(file.getId()) && 
                        !isUserOwnFile(targetUser, file)) {
                        
                        SmartRecommendation rec = createRecommendation(
                            targetUser, file.getId(), SmartRecommendation.RecommendationType.FILE,
                            "基于相似用户喜好推荐", 0.85, SmartRecommendation.SourceType.USER_BEHAVIOR);
                        recommendations.add(rec);
                        recommendedItemIds.add(file.getId());
                    }
                }
            }
            
            log.debug("协同过滤推荐生成: 用户={}, 推荐数量={}", targetUser.getUsername(), recommendations.size());
            
        } catch (Exception e) {
            log.warn("协同过滤推荐生成失败: 用户={}", targetUser.getUsername(), e);
        }
        
        return recommendations;
    }
    
    /**
     * 生成基于内容的推荐（文件相似性）
     */
    private List<SmartRecommendation> generateContentBasedRecommendations(User user) {
        List<SmartRecommendation> recommendations = new ArrayList<>();
        
        try {
            // 获取用户最近上传的文件
            List<FileEntity> userFiles = fileRepository.findByUploader(user, 
                PageRequest.of(0, 10)).getContent();
            
            if (userFiles.isEmpty()) {
                return recommendations;
            }
            
            // 获取所有文件进行相似性计算
            List<FileEntity> allFiles = fileRepository.findAll();
            Set<Long> userFileIds = userFiles.stream()
                .map(FileEntity::getId)
                .collect(Collectors.toSet());
            
            Map<FileEntity, Double> similarityScores = new HashMap<>();
            
            for (FileEntity userFile : userFiles) {
                for (FileEntity candidateFile : allFiles) {
                    if (!userFileIds.contains(candidateFile.getId()) && 
                        !isUserOwnFile(user, candidateFile)) {
                        
                        double similarity = calculateFileSimilarity(userFile, candidateFile);
                        if (similarity > SIMILARITY_THRESHOLD) {
                            similarityScores.merge(candidateFile, similarity, Math::max);
                        }
                    }
                }
            }
            
            // 按相似度排序并生成推荐
            similarityScores.entrySet().stream()
                .sorted(Map.Entry.<FileEntity, Double>comparingByValue().reversed())
                .limit(10)
                .forEach(entry -> {
                    SmartRecommendation rec = createRecommendation(
                        user, entry.getKey().getId(), SmartRecommendation.RecommendationType.FILE,
                        "基于内容相似性推荐", entry.getValue(), SmartRecommendation.SourceType.CONTENT_SIMILARITY);
                    recommendations.add(rec);
                });
            
            log.debug("内容基础推荐生成: 用户={}, 推荐数量={}", user.getUsername(), recommendations.size());
            
        } catch (Exception e) {
            log.warn("内容基础推荐生成失败: 用户={}", user.getUsername(), e);
        }
        
        return recommendations;
    }
    
    /**
     * 生成热门趋势推荐
     */
    private List<SmartRecommendation> generateTrendingRecommendations(User user) {
        List<SmartRecommendation> recommendations = new ArrayList<>();
        
        try {
            // 获取最近热门文件（按下载次数和分享次数排序）
            LocalDateTime recentPeriod = LocalDateTime.now().minusDays(30);
            Page<FileEntity> trendingFiles = fileRepository.findPublicFiles(PageRequest.of(0, 15));
            
            int rank = 1;
            for (FileEntity file : trendingFiles.getContent()) {
                if (!isUserOwnFile(user, file)) {
                    double score = Math.max(0.7, 1.0 - (rank * 0.05)); // 排名衰减
                    SmartRecommendation rec = createRecommendation(
                        user, file.getId(), SmartRecommendation.RecommendationType.FILE,
                        "热门文件推荐", score, SmartRecommendation.SourceType.USER_BEHAVIOR);
                    recommendations.add(rec);
                    rank++;
                }
            }
            
            log.debug("热门趋势推荐生成: 用户={}, 推荐数量={}", user.getUsername(), recommendations.size());
            
        } catch (Exception e) {
            log.warn("热门趋势推荐生成失败: 用户={}", user.getUsername(), e);
        }
        
        return recommendations;
    }
    
    /**
     * 生成基于标签的推荐
     */
    private List<SmartRecommendation> generateTagBasedRecommendations(User user) {
        List<SmartRecommendation> recommendations = new ArrayList<>();
        
        try {
            // 获取用户常用标签
            List<String> userTags = getUserFavoriteTags(user, 5);
            
            if (!userTags.isEmpty()) {
                // 根据标签查找相关文件
                for (String tagName : userTags) {
                    List<FileEntity> taggedFiles = findFilesByTag(tagName, 5);
                    
                    for (FileEntity file : taggedFiles) {
                        if (!isUserOwnFile(user, file)) {
                            SmartRecommendation rec = createRecommendation(
                                user, file.getId(), SmartRecommendation.RecommendationType.FILE,
                                "基于标签'" + tagName + "'的相关推荐", 0.75, 
                                SmartRecommendation.SourceType.CONTENT_SIMILARITY);
                            recommendations.add(rec);
                        }
                    }
                }
            }
            
            log.debug("标签基础推荐生成: 用户={}, 推荐数量={}", user.getUsername(), recommendations.size());
            
        } catch (Exception e) {
            log.warn("标签基础推荐生成失败: 用户={}", user.getUsername(), e);
        }
        
        return recommendations;
    }
    
    /**
     * 生成上下文感知推荐
     */
    private List<SmartRecommendation> generateContextualRecommendations(User user) {
        List<SmartRecommendation> recommendations = new ArrayList<>();
        
        try {
            LocalDateTime now = LocalDateTime.now();
            int hour = now.getHour();
            
            // 时间上下文推荐
            if (hour >= 9 && hour <= 12) {
                // 上午：工作相关文件
                recommendations.addAll(getWorkRelatedRecommendations(user));
            } else if (hour >= 14 && hour <= 18) {
                // 下午：学习和项目文件
                recommendations.addAll(getStudyRelatedRecommendations(user));
            } else if (hour >= 20 && hour <= 23) {
                // 晚上：娱乐和个人文件
                recommendations.addAll(getEntertainmentRecommendations(user));
            }
            
            // 工作日vs周末推荐
            if (now.getDayOfWeek().getValue() >= 6) { // 周末
                recommendations.addAll(getWeekendRecommendations(user));
            }
            
            log.debug("上下文推荐生成: 用户={}, 推荐数量={}", user.getUsername(), recommendations.size());
            
        } catch (Exception e) {
            log.warn("上下文推荐生成失败: 用户={}", user.getUsername(), e);
        }
        
        return recommendations;
    }
    
    // ==================== 辅助方法 ====================
    
    private List<User> findBehaviorSimilarUsers(User targetUser, int limit) {
        // 简化实现：返回随机用户作为示例
        // 实际应用中应基于用户行为向量计算余弦相似度
        return userRepository.findAll().stream()
            .filter(user -> !user.getId().equals(targetUser.getId()))
            .limit(limit)
            .collect(Collectors.toList());
    }
    
    private boolean isUserOwnFile(User user, FileEntity file) {
        return file.getUploader().getId().equals(user.getId());
    }
    
    private double calculateFileSimilarity(FileEntity file1, FileEntity file2) {
        double similarity = 0.0;
        
        // 扩展名相似度
        if (file1.getExtension().equalsIgnoreCase(file2.getExtension())) {
            similarity += 0.4;
        }
        
        // 文件大小相似度
        double sizeRatio = Math.min(file1.getFileSize(), file2.getFileSize()) * 1.0 / 
                          Math.max(file1.getFileSize(), file2.getFileSize());
        similarity += sizeRatio * 0.3;
        
        // 标签相似度
        List<String> commonTags = findCommonTags(file1, file2);
        similarity += Math.min(commonTags.size() * 0.1, 0.3);
        
        return Math.min(similarity, 1.0);
    }
    
    private List<String> findCommonTags(FileEntity file1, FileEntity file2) {
        // 简化实现
        return Arrays.asList("important", "document");
    }
    
    private List<String> getUserFavoriteTags(User user, int limit) {
        // 简化实现：返回常用标签
        return Arrays.asList("work", "study", "personal");
    }
    
    private List<FileEntity> findFilesByTag(String tagName, int limit) {
        // 简化实现：返回示例文件
        return fileRepository.findAll().stream().limit(limit).collect(Collectors.toList());
    }
    
    private SmartRecommendation createRecommendation(User user, Long itemId, 
        SmartRecommendation.RecommendationType type, String reason, 
        double relevanceScore, SmartRecommendation.SourceType sourceType) {
        
        SmartRecommendation recommendation = new SmartRecommendation();
        recommendation.setUser(user);
        recommendation.setRecommendationType(type);
        recommendation.setItemId(itemId);
        recommendation.setReason(reason);
        recommendation.setRelevanceScore(relevanceScore);
        recommendation.setSourceType(sourceType);
        recommendation.setIsViewed(false);
        recommendation.setIsAdopted(false);
        recommendation.setCreatedAt(LocalDateTime.now());
        recommendation.setExpireAt(LocalDateTime.now().plusDays(RECOMMENDATION_EXPIRE_DAYS));
        
        return recommendation;
    }
    
    private List<SmartRecommendation> deduplicateAndSort(List<SmartRecommendation> recommendations) {
        // 按相关度分数降序排列，并去除重复项
        return recommendations.stream()
            .collect(Collectors.toMap(
                rec -> rec.getRecommendationType() + "_" + rec.getItemId(),
                rec -> rec,
                (existing, replacement) -> existing.getRelevanceScore() >= replacement.getRelevanceScore() ? existing : replacement
            ))
            .values()
            .stream()
            .sorted(Comparator.comparing(SmartRecommendation::getRelevanceScore).reversed())
            .collect(Collectors.toList());
    }
    
    private List<SmartRecommendation> getSimilarFiles(Long fileId, User user) {
        // 实现相似文件推荐逻辑
        return new ArrayList<>();
    }
    
    private List<SmartRecommendation> getSimilarFolders(Long folderId, User user) {
        // 实现相似文件夹推荐逻辑
        return new ArrayList<>();
    }
    
    private List<SmartRecommendation> getRelatedTags(Long tagId, User user) {
        // 实现相关标签推荐逻辑
        return new ArrayList<>();
    }
    
    private List<SmartRecommendation> getWorkRelatedRecommendations(User user) {
        return new ArrayList<>();
    }
    
    private List<SmartRecommendation> getStudyRelatedRecommendations(User user) {
        return new ArrayList<>();
    }
    
    private List<SmartRecommendation> getEntertainmentRecommendations(User user) {
        return new ArrayList<>();
    }
    
    private List<SmartRecommendation> getWeekendRecommendations(User user) {
        return new ArrayList<>();
    }
}