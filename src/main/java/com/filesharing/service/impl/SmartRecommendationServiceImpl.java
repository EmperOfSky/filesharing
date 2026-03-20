package com.filesharing.service.impl;

import com.filesharing.entity.*;
import com.filesharing.exception.BusinessException;
import com.filesharing.repository.*;
import com.filesharing.service.SmartRecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
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
    private static final double SIMILARITY_THRESHOLD = 0.45;
    private static final int MAX_RECOMMENDATIONS = 20;
    private static final int RECOMMENDATION_EXPIRE_DAYS = 7;
    private static final double DECAY_FACTOR = 0.95;
    private static final int MAX_RECENT_USER_FILES = 30;
    private static final int MAX_PUBLIC_CANDIDATES = 200;
    
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
        if (!Boolean.TRUE.equals(recommendation.getIsViewed())) {
            recommendation.setIsViewed(true);
            recommendation.setViewedAt(LocalDateTime.now());
        }
        smartRecommendationRepository.save(recommendation);
        
        log.info("标记推荐已采纳: 用户={}, 推荐ID={}, 类型={}", 
            user.getUsername(), recommendationId, recommendation.getRecommendationType());
    }
    
    @Override
    @Transactional
    public void cleanupExpiredRecommendations() {
        // 过期清理应以当前时间为准，删除所有已过期记录。
        LocalDateTime expiredDate = LocalDateTime.now();
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
            UserBehaviorStatistics targetStats = userBehaviorStatisticsRepository.findByUser(targetUser).orElse(null);
            
            // 收集相似用户喜欢的内容
            Set<Long> recommendedItemIds = new HashSet<>();
            
            for (User similarUser : similarUsers) {
                UserBehaviorStatistics similarStats = userBehaviorStatisticsRepository.findByUser(similarUser).orElse(null);
                double similarity = calculateBehaviorSimilarity(targetUser, targetStats, similarUser, similarStats);
                if (similarity < 0.25) {
                    continue;
                }

                // 获取相似用户最近互动的文件
                List<FileEntity> similarUserFiles = fileRepository.findByUploader(similarUser, 
                    PageRequest.of(0, 20)).getContent();
                
                for (FileEntity file : similarUserFiles) {
                    if (!recommendedItemIds.contains(file.getId()) && isRecommendableFile(targetUser, file)) {
                        
                        double score = clampScore(0.55 + similarity * 0.3 + calculatePopularityScore(file) * 0.15);
                        SmartRecommendation rec = createRecommendation(
                            targetUser, file.getId(), SmartRecommendation.RecommendationType.FILE,
                            "与你行为相似的用户近期关注了该文件", score,
                            SmartRecommendation.SourceType.USER_BEHAVIOR);
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
                PageRequest.of(0, MAX_RECENT_USER_FILES)).getContent().stream()
                .filter(this::isAvailableFile)
                .collect(Collectors.toList());
            
            if (userFiles.isEmpty()) {
                return recommendations;
            }
            
            // 获取所有文件进行相似性计算
            List<FileEntity> allFiles = fileRepository.findPublicFiles(PageRequest.of(0, MAX_PUBLIC_CANDIDATES))
                .getContent();
            Set<Long> userFileIds = userFiles.stream()
                .map(FileEntity::getId)
                .collect(Collectors.toSet());
            
            Map<FileEntity, Double> similarityScores = new HashMap<>();
            Map<FileEntity, FileEntity> bestAnchor = new HashMap<>();
            Map<Long, Set<String>> tagCache = buildTagCache(userFiles, allFiles);
            
            for (FileEntity userFile : userFiles) {
                for (FileEntity candidateFile : allFiles) {
                    if (!userFileIds.contains(candidateFile.getId()) && isRecommendableFile(user, candidateFile)) {
                        
                        double similarity = calculateFileSimilarity(userFile, candidateFile, tagCache);
                        if (similarity > SIMILARITY_THRESHOLD) {
                            similarityScores.merge(candidateFile, similarity, Math::max);
                            if (!bestAnchor.containsKey(candidateFile) ||
                                similarity > similarityScores.getOrDefault(candidateFile, 0.0)) {
                                bestAnchor.put(candidateFile, userFile);
                            }
                        }
                    }
                }
            }
            
            // 按相似度排序并生成推荐
            similarityScores.entrySet().stream()
                .sorted(Map.Entry.<FileEntity, Double>comparingByValue().reversed())
                .limit(10)
                .forEach(entry -> {
                    FileEntity anchor = bestAnchor.get(entry.getKey());
                    double score = clampScore(entry.getValue() * 0.75 + calculatePopularityScore(entry.getKey()) * 0.25);
                    SmartRecommendation rec = createRecommendation(
                        user, entry.getKey().getId(), SmartRecommendation.RecommendationType.FILE,
                        buildSimilarityReason(anchor, entry.getKey()), score,
                        SmartRecommendation.SourceType.CONTENT_SIMILARITY);
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
            List<FileEntity> trendingFiles = fileRepository.findPublicFiles(PageRequest.of(0, MAX_PUBLIC_CANDIDATES))
                .getContent().stream()
                .filter(file -> isRecommendableFile(user, file))
                .sorted(Comparator.comparing(this::calculatePopularityScore).reversed())
                .limit(12)
                .collect(Collectors.toList());

            int rank = 0;
            for (FileEntity file : trendingFiles) {
                double popularity = calculatePopularityScore(file);
                double rankPenalty = Math.max(0.0, 1.0 - rank * 0.04);
                double score = clampScore(0.55 + popularity * 0.3 + rankPenalty * 0.15);
                    SmartRecommendation rec = createRecommendation(
                        user, file.getId(), SmartRecommendation.RecommendationType.FILE,
                        buildTrendingReason(file), score, SmartRecommendation.SourceType.USER_BEHAVIOR);
                    recommendations.add(rec);
                rank++;
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
            List<String> userTags = getUserFavoriteTags(user, 6);
            
            if (!userTags.isEmpty()) {
                Map<String, Double> tagWeights = new LinkedHashMap<>();
                for (int i = 0; i < userTags.size(); i++) {
                    tagWeights.put(userTags.get(i), 1.0 - i * 0.1);
                }

                Map<Long, Double> itemScore = new HashMap<>();
                Map<Long, String> reasonTag = new HashMap<>();

                // 根据标签查找相关文件
                for (String tagName : userTags) {
                    List<FileEntity> taggedFiles = findFilesByTag(tagName, 5);
                    
                    for (FileEntity file : taggedFiles) {
                        if (isRecommendableFile(user, file)) {
                            double increment = clampScore(tagWeights.getOrDefault(tagName, 0.5) * 0.5 +
                                calculatePopularityScore(file) * 0.5);
                            itemScore.merge(file.getId(), increment, Math::max);
                            reasonTag.putIfAbsent(file.getId(), tagName);
                        }
                    }
                }

                itemScore.entrySet().stream()
                    .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                    .limit(10)
                    .forEach(entry -> {
                        SmartRecommendation rec = createRecommendation(
                            user,
                            entry.getKey(),
                            SmartRecommendation.RecommendationType.FILE,
                            "与你常用标签'" + reasonTag.get(entry.getKey()) + "'相关",
                            clampScore(entry.getValue()),
                            SmartRecommendation.SourceType.CONTENT_SIMILARITY
                        );
                        recommendations.add(rec);
                    });
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
        UserBehaviorStatistics targetStats = userBehaviorStatisticsRepository.findByUser(targetUser).orElse(null);

        return userRepository.findAll().stream()
            .filter(user -> !Objects.equals(user.getId(), targetUser.getId()))
            .map(user -> new AbstractMap.SimpleEntry<>(
                user,
                calculateBehaviorSimilarity(
                    targetUser,
                    targetStats,
                    user,
                    userBehaviorStatisticsRepository.findByUser(user).orElse(null)
                )
            ))
            .filter(entry -> entry.getValue() >= 0.25)
            .sorted(Map.Entry.<User, Double>comparingByValue().reversed())
            .limit(limit)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }
    
    private boolean isUserOwnFile(User user, FileEntity file) {
        return file != null && file.getUploader() != null && Objects.equals(file.getUploader().getId(), user.getId());
    }
    
    private double calculateFileSimilarity(FileEntity file1, FileEntity file2, Map<Long, Set<String>> tagCache) {
        double similarity = 0.0;
        
        // 扩展名相似度
        if (Objects.equals(normalize(file1.getExtension()), normalize(file2.getExtension()))) {
            similarity += 0.35;
        }

        // MIME 主类型相似度
        String type1 = extractMimePrimaryType(file1.getContentType());
        String type2 = extractMimePrimaryType(file2.getContentType());
        if (!type1.isEmpty() && type1.equals(type2)) {
            similarity += 0.15;
        }

        // 文件大小相似度
        long size1 = nullSafeLong(file1.getFileSize());
        long size2 = nullSafeLong(file2.getFileSize());
        if (size1 > 0 && size2 > 0) {
            double sizeRatio = Math.min(size1, size2) * 1.0 / Math.max(size1, size2);
            similarity += sizeRatio * 0.2;
        }
        
        // 标签相似度
        Set<String> tags1 = tagCache.getOrDefault(file1.getId(), Collections.emptySet());
        Set<String> tags2 = tagCache.getOrDefault(file2.getId(), Collections.emptySet());
        similarity += calculateSetSimilarity(tags1, tags2) * 0.2;

        // 新旧程度相似度
        similarity += calculateRecencySimilarity(file1.getCreatedAt(), file2.getCreatedAt()) * 0.1;
        
        return clampScore(similarity);
    }
    
    private List<String> findCommonTags(FileEntity file1, FileEntity file2) {
        Set<String> tags1 = fileTagRepository.findTagNamesByFile(file1).stream()
            .map(this::normalize)
            .filter(tag -> !tag.isEmpty())
            .collect(Collectors.toSet());
        Set<String> tags2 = fileTagRepository.findTagNamesByFile(file2).stream()
            .map(this::normalize)
            .filter(tag -> !tag.isEmpty())
            .collect(Collectors.toSet());

        return tags1.stream().filter(tags2::contains).collect(Collectors.toList());
    }
    
    private List<String> getUserFavoriteTags(User user, int limit) {
        List<FileEntity> userFiles = fileRepository.findByUploader(user, PageRequest.of(0, MAX_RECENT_USER_FILES))
            .getContent();

        if (userFiles.isEmpty()) {
            return tagRepository.findPopularTags(user, PageRequest.of(0, limit)).getContent().stream()
                .map(Tag::getTagName)
                .filter(Objects::nonNull)
                .limit(limit)
                .collect(Collectors.toList());
        }

        Map<String, Long> usage = new HashMap<>();
        for (FileEntity file : userFiles) {
            for (String tag : fileTagRepository.findTagNamesByFile(file)) {
                String normalizedTag = normalize(tag);
                if (!normalizedTag.isEmpty()) {
                    usage.merge(normalizedTag, 1L, Long::sum);
                }
            }
        }

        return usage.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(limit)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }
    
    private List<FileEntity> findFilesByTag(String tagName, int limit) {
        return tagRepository.findByTagName(tagName)
            .map(tag -> fileTagRepository.findFilesByTag(tag).stream()
                .filter(this::isAvailableFile)
                .limit(limit)
                .collect(Collectors.toList()))
            .orElseGet(Collections::emptyList);
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
        Optional<FileEntity> baseFileOpt = fileRepository.findById(fileId);
        if (baseFileOpt.isEmpty()) {
            return Collections.emptyList();
        }

        FileEntity baseFile = baseFileOpt.get();
        List<FileEntity> candidates = fileRepository.findPublicFiles(PageRequest.of(0, MAX_PUBLIC_CANDIDATES))
            .getContent();

        Map<Long, Set<String>> tagCache = buildTagCache(Collections.singletonList(baseFile), candidates);

        return candidates.stream()
            .filter(file -> !Objects.equals(file.getId(), baseFile.getId()))
            .filter(file -> isRecommendableFile(user, file))
            .map(file -> new AbstractMap.SimpleEntry<>(file, calculateFileSimilarity(baseFile, file, tagCache)))
            .filter(entry -> entry.getValue() > SIMILARITY_THRESHOLD)
            .sorted(Map.Entry.<FileEntity, Double>comparingByValue().reversed())
            .limit(10)
            .map(entry -> createRecommendation(
                user,
                entry.getKey().getId(),
                SmartRecommendation.RecommendationType.FILE,
                buildSimilarityReason(baseFile, entry.getKey()),
                clampScore(entry.getValue() * 0.8 + calculatePopularityScore(entry.getKey()) * 0.2),
                SmartRecommendation.SourceType.CONTENT_SIMILARITY
            ))
            .collect(Collectors.toList());
    }
    
    private List<SmartRecommendation> getSimilarFolders(Long folderId, User user) {
        Optional<Folder> baseFolderOpt = folderRepository.findById(folderId);
        List<Folder> publicFolders = folderRepository.findPublicFolders().stream()
            .filter(folder -> folder.getOwner() != null && !Objects.equals(folder.getOwner().getId(), user.getId()))
            .collect(Collectors.toList());

        if (publicFolders.isEmpty()) {
            return Collections.emptyList();
        }

        return publicFolders.stream()
            .map(folder -> {
                double score;
                String reason;

                if (baseFolderOpt.isPresent()) {
                    Folder baseFolder = baseFolderOpt.get();
                    Set<String> baseTokens = tokenizeName(baseFolder.getName());
                    Set<String> candidateTokens = tokenizeName(folder.getName());
                    double nameSimilarity = calculateSetSimilarity(baseTokens, candidateTokens);
                    double recency = calculateRecencyScore(folder.getCreatedAt());
                    score = clampScore(nameSimilarity * 0.7 + recency * 0.3);
                    reason = "与当前文件夹命名风格相近";
                } else {
                    score = clampScore(0.5 + calculateRecencyScore(folder.getCreatedAt()) * 0.5);
                    reason = "公开且近期活跃的共享文件夹";
                }

                return createRecommendation(
                    user,
                    folder.getId(),
                    SmartRecommendation.RecommendationType.FOLDER,
                    reason,
                    score,
                    SmartRecommendation.SourceType.CONTENT_SIMILARITY
                );
            })
            .filter(rec -> rec.getRelevanceScore() >= 0.35)
            .sorted(Comparator.comparing(SmartRecommendation::getRelevanceScore).reversed())
            .limit(8)
            .collect(Collectors.toList());
    }
    
    private List<SmartRecommendation> getRelatedTags(Long tagId, User user) {
        Optional<Tag> baseTagOpt = tagRepository.findById(tagId);
        if (baseTagOpt.isEmpty()) {
            return Collections.emptyList();
        }

        Tag baseTag = baseTagOpt.get();
        List<FileEntity> relatedFiles = fileTagRepository.findFilesByTag(baseTag);
        if (relatedFiles.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Tag, Long> coOccurrence = new HashMap<>();
        for (FileEntity file : relatedFiles) {
            for (Tag tag : fileTagRepository.findTagsByFile(file)) {
                if (!Objects.equals(tag.getId(), baseTag.getId())) {
                    coOccurrence.merge(tag, 1L, Long::sum);
                }
            }
        }

        long total = relatedFiles.size();
        return coOccurrence.entrySet().stream()
            .sorted(Map.Entry.<Tag, Long>comparingByValue().reversed())
            .limit(8)
            .map(entry -> createRecommendation(
                user,
                entry.getKey().getId(),
                SmartRecommendation.RecommendationType.TAG,
                "与标签'" + baseTag.getTagName() + "'经常共同出现",
                clampScore(entry.getValue() * 1.0 / total),
                SmartRecommendation.SourceType.CONTENT_SIMILARITY
            ))
            .collect(Collectors.toList());
    }
    
    private List<SmartRecommendation> getWorkRelatedRecommendations(User user) {
        return recommendByExtensionSet(
            user,
            new HashSet<>(Arrays.asList("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx")),
            "工作时段常用文档类型"
        );
    }
    
    private List<SmartRecommendation> getStudyRelatedRecommendations(User user) {
        return recommendByExtensionSet(
            user,
            new HashSet<>(Arrays.asList("md", "txt", "java", "py", "js", "ts", "sql")),
            "学习/项目时段常用资料类型"
        );
    }
    
    private List<SmartRecommendation> getEntertainmentRecommendations(User user) {
        return recommendByExtensionSet(
            user,
            new HashSet<>(Arrays.asList("jpg", "jpeg", "png", "gif", "mp3", "wav", "mp4", "mov")),
            "晚间偏好的娱乐内容"
        );
    }
    
    private List<SmartRecommendation> getWeekendRecommendations(User user) {
        return recommendByExtensionSet(
            user,
            new HashSet<>(Arrays.asList("zip", "rar", "7z", "mp4", "pdf", "md")),
            "周末常用内容类型"
        );
    }

    private List<SmartRecommendation> recommendByExtensionSet(User user, Set<String> preferredExtensions, String reasonPrefix) {
        if (preferredExtensions.isEmpty()) {
            return Collections.emptyList();
        }

        return fileRepository.findPublicFiles(PageRequest.of(0, MAX_PUBLIC_CANDIDATES)).getContent().stream()
            .filter(file -> isRecommendableFile(user, file))
            .filter(file -> preferredExtensions.contains(normalize(file.getExtension())))
            .sorted(Comparator.comparing(this::calculatePopularityScore).reversed())
            .limit(6)
            .map(file -> createRecommendation(
                user,
                file.getId(),
                SmartRecommendation.RecommendationType.FILE,
                reasonPrefix + "（." + normalize(file.getExtension()) + "）",
                clampScore(0.45 + calculatePopularityScore(file) * 0.35 + calculateRecencyScore(file.getCreatedAt()) * 0.2),
                SmartRecommendation.SourceType.AI_MODEL
            ))
            .collect(Collectors.toList());
    }

    private boolean isRecommendableFile(User user, FileEntity file) {
        return file != null
            && file.getId() != null
            && isAvailableFile(file)
            && Boolean.TRUE.equals(file.getIsPublic())
            && !isUserOwnFile(user, file);
    }

    private boolean isAvailableFile(FileEntity file) {
        return file != null && FileEntity.FileStatus.AVAILABLE.equals(file.getStatus());
    }

    private double calculatePopularityScore(FileEntity file) {
        double download = Math.min(1.0, nullSafeInt(file.getDownloadCount()) / 50.0);
        double preview = Math.min(1.0, nullSafeInt(file.getPreviewCount()) / 100.0);
        double share = Math.min(1.0, nullSafeInt(file.getShareCount()) / 20.0);
        double activity = download * 0.5 + preview * 0.2 + share * 0.3;

        long ageDays = Math.max(0, ChronoUnit.DAYS.between(
            Optional.ofNullable(file.getCreatedAt()).orElse(LocalDateTime.now()),
            LocalDateTime.now()
        ));
        double decay = Math.pow(DECAY_FACTOR, ageDays / 7.0);
        return clampScore(activity * decay + 0.1);
    }

    private String buildTrendingReason(FileEntity file) {
        return "近期热度较高（下载" + nullSafeInt(file.getDownloadCount()) +
            "、预览" + nullSafeInt(file.getPreviewCount()) +
            "、分享" + nullSafeInt(file.getShareCount()) + "）";
    }

    private String buildSimilarityReason(FileEntity anchor, FileEntity candidate) {
        if (anchor == null || candidate == null) {
            return "与您的历史内容偏好相似";
        }

        String anchorExt = normalize(anchor.getExtension());
        String candidateExt = normalize(candidate.getExtension());
        if (!anchorExt.isEmpty() && anchorExt.equals(candidateExt)) {
            return "与您近期文件类型一致（." + anchorExt + "）";
        }

        List<String> common = findCommonTags(anchor, candidate);
        if (!common.isEmpty()) {
            return "与您内容标签相近（" + common.get(0) + "）";
        }

        return "与您近期访问内容在格式和体量上相似";
    }

    private Map<Long, Set<String>> buildTagCache(List<FileEntity> sourceFiles, List<FileEntity> candidateFiles) {
        Map<Long, Set<String>> cache = new HashMap<>();
        List<FileEntity> allFiles = new ArrayList<>();
        allFiles.addAll(sourceFiles);
        allFiles.addAll(candidateFiles);

        for (FileEntity file : allFiles) {
            if (file == null || file.getId() == null || cache.containsKey(file.getId())) {
                continue;
            }
            Set<String> tags = fileTagRepository.findTagNamesByFile(file).stream()
                .map(this::normalize)
                .filter(tag -> !tag.isEmpty())
                .collect(Collectors.toSet());
            cache.put(file.getId(), tags);
        }

        return cache;
    }

    private double calculateBehaviorSimilarity(User targetUser,
                                               UserBehaviorStatistics targetStats,
                                               User candidateUser,
                                               UserBehaviorStatistics candidateStats) {
        double statsSimilarity = calculateStatsSimilarity(targetStats, candidateStats);
        double fileTypeSimilarity = calculateUserFileTypeSimilarity(targetUser, candidateUser);
        return clampScore(statsSimilarity * 0.65 + fileTypeSimilarity * 0.35);
    }

    private double calculateStatsSimilarity(UserBehaviorStatistics left, UserBehaviorStatistics right) {
        if (left == null || right == null) {
            return 0.0;
        }

        double uploads = normalizedDistanceSimilarity(left.getTotalUploads(), right.getTotalUploads());
        double downloads = normalizedDistanceSimilarity(left.getTotalDownloads(), right.getTotalDownloads());
        double previews = normalizedDistanceSimilarity(left.getTotalPreviews(), right.getTotalPreviews());
        double shares = normalizedDistanceSimilarity(left.getTotalShares(), right.getTotalShares());
        double fileSize = normalizedDistanceSimilarity(left.getAverageFileSize(), right.getAverageFileSize());

        double favoriteType = Objects.equals(normalize(left.getFavoriteFileType()), normalize(right.getFavoriteFileType()))
            ? 1.0 : 0.0;

        return clampScore(uploads * 0.2 + downloads * 0.2 + previews * 0.2 + shares * 0.15 + fileSize * 0.15 + favoriteType * 0.1);
    }

    private double calculateUserFileTypeSimilarity(User user1, User user2) {
        Map<String, Long> user1Types = getUserFileTypeProfile(user1);
        Map<String, Long> user2Types = getUserFileTypeProfile(user2);
        return calculateSetSimilarity(user1Types.keySet(), user2Types.keySet());
    }

    private Map<String, Long> getUserFileTypeProfile(User user) {
        return fileRepository.findByUploader(user, PageRequest.of(0, MAX_RECENT_USER_FILES)).getContent().stream()
            .map(FileEntity::getExtension)
            .map(this::normalize)
            .filter(ext -> !ext.isEmpty())
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    }

    private double calculateSetSimilarity(Set<String> left, Set<String> right) {
        if (left == null || right == null || left.isEmpty() || right.isEmpty()) {
            return 0.0;
        }
        Set<String> union = new HashSet<>(left);
        union.addAll(right);
        Set<String> intersection = new HashSet<>(left);
        intersection.retainAll(right);
        return union.isEmpty() ? 0.0 : intersection.size() * 1.0 / union.size();
    }

    private double calculateRecencyScore(LocalDateTime createdAt) {
        if (createdAt == null) {
            return 0.0;
        }
        long days = Math.max(0, ChronoUnit.DAYS.between(createdAt, LocalDateTime.now()));
        return clampScore(Math.pow(DECAY_FACTOR, days / 3.0));
    }

    private double calculateRecencySimilarity(LocalDateTime left, LocalDateTime right) {
        if (left == null || right == null) {
            return 0.0;
        }
        long dayDiff = Math.abs(ChronoUnit.DAYS.between(left, right));
        return clampScore(1.0 / (1.0 + dayDiff / 7.0));
    }

    private double normalizedDistanceSimilarity(Long left, Long right) {
        long leftValue = nullSafeLong(left);
        long rightValue = nullSafeLong(right);
        long max = Math.max(leftValue, rightValue);
        if (max == 0L) {
            return 1.0;
        }
        return clampScore(1.0 - Math.abs(leftValue - rightValue) * 1.0 / max);
    }

    private int nullSafeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private long nullSafeLong(Long value) {
        return value == null ? 0L : value;
    }

    private double clampScore(double score) {
        return Math.max(0.0, Math.min(1.0, score));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String extractMimePrimaryType(String contentType) {
        if (contentType == null || !contentType.contains("/")) {
            return "";
        }
        return normalize(contentType.substring(0, contentType.indexOf('/')));
    }

    private Set<String> tokenizeName(String name) {
        if (name == null || name.isBlank()) {
            return Collections.emptySet();
        }
        return Arrays.stream(name.toLowerCase(Locale.ROOT).split("[^a-z0-9\\u4e00-\\u9fa5]+"))
            .filter(token -> !token.isBlank())
            .collect(Collectors.toSet());
    }
}