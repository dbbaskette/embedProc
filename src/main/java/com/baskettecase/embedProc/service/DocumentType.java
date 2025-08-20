package com.baskettecase.embedProc.service;

/**
 * Enum representing different document types based on their source path
 */
public enum DocumentType {
    POLICY("policy"),
    REFERENCE("reference"),
    UNKNOWN("unknown");
    
    private final String value;
    
    DocumentType(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    /**
     * Determine document type from file URL/path
     * @param fileUrl The file URL or path
     * @return The corresponding DocumentType
     */
    public static DocumentType fromUrl(String fileUrl) {
        if (fileUrl == null) {
            return UNKNOWN;
        }
        
        String lowerUrl = fileUrl.toLowerCase();
        if (lowerUrl.contains("/policies")) {
            return POLICY;
        } else if (lowerUrl.contains("/reference")) {
            return REFERENCE;
        } else {
            return UNKNOWN;
        }
    }
}
