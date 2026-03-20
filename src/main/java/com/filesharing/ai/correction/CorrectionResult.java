package com.filesharing.ai.correction;

import java.util.List;

/**
 * 文本纠错结果DTO
 */
public class CorrectionResult {
    private String originalText;
    private String correctedText;
    private List<CorrectionSuggestion> suggestions;
    private Double confidence;
    private String error;
    
    // Constructors
    public CorrectionResult() {}
    
    public CorrectionResult(String originalText, String correctedText, Double confidence) {
        this.originalText = originalText;
        this.correctedText = correctedText;
        this.confidence = confidence;
    }
    
    // Getters and Setters
    public String getOriginalText() { return originalText; }
    public void setOriginalText(String originalText) { this.originalText = originalText; }
    
    public String getCorrectedText() { return correctedText; }
    public void setCorrectedText(String correctedText) { this.correctedText = correctedText; }
    
    public List<CorrectionSuggestion> getSuggestions() { return suggestions; }
    public void setSuggestions(List<CorrectionSuggestion> suggestions) { this.suggestions = suggestions; }
    
    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }
    
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    
    public boolean isSuccess() {
        return error == null || error.isEmpty();
    }
}