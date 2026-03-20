package com.filesharing.ai.correction;

/**
 * 纠错建议DTO
 */
public class CorrectionSuggestion {
    private String originalText;
    private String suggestedText;
    private Double confidence;
    private String description;
    private Integer position;
    
    // Constructors
    public CorrectionSuggestion() {}
    
    public CorrectionSuggestion(String originalText, String suggestedText, Double confidence) {
        this.originalText = originalText;
        this.suggestedText = suggestedText;
        this.confidence = confidence;
    }
    
    // Getters and Setters
    public String getOriginalText() { return originalText; }
    public void setOriginalText(String originalText) { this.originalText = originalText; }
    
    public String getSuggestedText() { return suggestedText; }
    public void setSuggestedText(String suggestedText) { this.suggestedText = suggestedText; }
    
    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Integer getPosition() { return position; }
    public void setPosition(Integer position) { this.position = position; }
}