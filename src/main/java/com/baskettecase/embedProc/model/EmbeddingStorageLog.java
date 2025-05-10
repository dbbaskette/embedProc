package com.baskettecase.embedProc.model;

import java.time.Instant;

public class EmbeddingStorageLog {
    private String textPreview;
    private int embeddingSize;
    private Instant timestamp;
    private String status;
    private String errorMessage;
    private String id;

    public EmbeddingStorageLog() {}

    public EmbeddingStorageLog(String textPreview, int embeddingSize, Instant timestamp, String status, String errorMessage, String id) {
        this.textPreview = textPreview;
        this.embeddingSize = embeddingSize;
        this.timestamp = timestamp;
        this.status = status;
        this.errorMessage = errorMessage;
        this.id = id;
    }

    public String getTextPreview() { return textPreview; }
    public void setTextPreview(String textPreview) { this.textPreview = textPreview; }
    public int getEmbeddingSize() { return embeddingSize; }
    public void setEmbeddingSize(int embeddingSize) { this.embeddingSize = embeddingSize; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
}
