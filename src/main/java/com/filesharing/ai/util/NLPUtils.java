package com.filesharing.ai.util;

import lombok.extern.slf4j.Slf4j;
import java.util.*;
import java.util.stream.Collectors;

/**
 * NLP工具类
 * 提供自然语言处理相关功能
 */
@Slf4j
public class NLPUtils {
    
    // 停用词集合
    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
        "的", "了", "在", "是", "我", "有", "和", "就", "不", "人", "都", "一", "一个",
        "上", "也", "很", "到", "说", "要", "去", "你", "会", "着", "没有", "看", "好",
        "自己", "这", "那", "里", "就是", "把", "比", "从", "被", "本", "来", "们",
        "the", "and", "or", "but", "in", "on", "at", "to", "for", "of", "with", "by",
        "is", "are", "was", "were", "be", "been", "have", "has", "had", "do", "does",
        "did", "will", "would", "could", "should", "may", "might", "must", "can"
    ));
    
    /**
     * 分词处理
     */
    public static List<String> tokenize(String text) {
        if (text == null || text.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 简单的分词处理
        return Arrays.stream(text.split("[\\s\\p{Punct}]+"))
                .filter(word -> !word.isEmpty())
                .collect(Collectors.toList());
    }
    
    /**
     * 移除停用词
     */
    public static List<String> removeStopWords(List<String> words) {
        return words.stream()
                .filter(word -> !STOP_WORDS.contains(word.toLowerCase()))
                .collect(Collectors.toList());
    }
    
    /**
     * 计算TF-IDF权重
     */
    public static Map<String, Double> calculateTFIDF(List<String> documents, String targetDoc) {
        Map<String, Double> tfidf = new HashMap<>();
        
        // 计算词频(TF)
        String[] words = targetDoc.toLowerCase().split("[\\s\\p{Punct}]+");
        Map<String, Integer> termFreq = new HashMap<>();
        for (String word : words) {
            if (!word.isEmpty() && !STOP_WORDS.contains(word)) {
                termFreq.put(word, termFreq.getOrDefault(word, 0) + 1);
            }
        }
        
        // 计算逆文档频率(IDF)
        int totalDocs = documents.size();
        Map<String, Integer> docFreq = new HashMap<>();
        
        for (String doc : documents) {
            Set<String> uniqueWords = new HashSet<>(
                Arrays.asList(doc.toLowerCase().split("[\\s\\p{Punct}]+"))
            );
            for (String word : uniqueWords) {
                if (!word.isEmpty() && !STOP_WORDS.contains(word)) {
                    docFreq.put(word, docFreq.getOrDefault(word, 0) + 1);
                }
            }
        }
        
        // 计算TF-IDF
        for (Map.Entry<String, Integer> entry : termFreq.entrySet()) {
            String term = entry.getKey();
            double tf = (double) entry.getValue() / words.length;
            double idf = Math.log((double) totalDocs / (1 + docFreq.getOrDefault(term, 0)));
            tfidf.put(term, tf * idf);
        }
        
        return tfidf;
    }
    
    /**
     * 计算余弦相似度
     */
    public static double cosineSimilarity(Map<String, Double> vector1, Map<String, Double> vector2) {
        Set<String> allTerms = new HashSet<>();
        allTerms.addAll(vector1.keySet());
        allTerms.addAll(vector2.keySet());
        
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        
        for (String term : allTerms) {
            double val1 = vector1.getOrDefault(term, 0.0);
            double val2 = vector2.getOrDefault(term, 0.0);
            
            dotProduct += val1 * val2;
            norm1 += val1 * val1;
            norm2 += val2 * val2;
        }
        
        if (norm1 == 0 || norm2 == 0) {
            return 0.0;
        }
        
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
    
    /**
     * 提取名词短语（简化版）
     */
    public static List<String> extractNounPhrases(String text) {
        List<String> phrases = new ArrayList<>();
        String[] words = text.split("[\\s\\p{Punct}]+");
        
        // 简单的名词短语提取规则
        for (int i = 0; i < words.length - 1; i++) {
            String word1 = words[i];
            String word2 = words[i + 1];
            
            // 简单规则：形容词+名词 或 名词+名词
            if (word1.length() > 1 && word2.length() > 1) {
                phrases.add(word1 + " " + word2);
            }
        }
        
        return phrases;
    }
    
    /**
     * 文本清洗
     */
    public static String cleanText(String text) {
        if (text == null) {
            return "";
        }
        
        return text
                .replaceAll("[\\r\\n\\t]", " ")  // 替换换行符和制表符
                .replaceAll("\\s+", " ")        // 规范化空白字符
                .trim();
    }
    
    /**
     * 计算文本统计信息
     */
    public static Map<String, Object> getTextStatistics(String text) {
        Map<String, Object> stats = new HashMap<>();
        
        if (text == null || text.isEmpty()) {
            stats.put("characterCount", 0);
            stats.put("wordCount", 0);
            stats.put("sentenceCount", 0);
            stats.put("paragraphCount", 0);
            return stats;
        }
        
        stats.put("characterCount", text.length());
        stats.put("wordCount", tokenize(text).size());
        stats.put("sentenceCount", text.split("[.!?。！？]+").length);
        stats.put("paragraphCount", text.split("\\n\\s*\\n").length);
        
        return stats;
    }
}