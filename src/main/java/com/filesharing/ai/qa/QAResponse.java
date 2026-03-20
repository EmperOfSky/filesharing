package com.filesharing.ai.qa;

import java.util.List;

/**
 * 问答系统响应DTO
 */
public class QAResponse {
    private String question;
    private String answer;
    private Double confidence;
    private List<String> sources;
    private String error;
    
    // Constructors
    public QAResponse() {}
    
    public QAResponse(String question, String answer, Double confidence) {
        this.question = question;
        this.answer = answer;
        this.confidence = confidence;
    }
    
    // Getters and Setters
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    
    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    
    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }
    
    public List<String> getSources() { return sources; }
    public void setSources(List<String> sources) { this.sources = sources; }
    
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    
    public boolean isSuccess() {
        return error == null || error.isEmpty();
    }
}