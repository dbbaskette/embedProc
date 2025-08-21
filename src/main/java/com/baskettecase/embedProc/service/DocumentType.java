package com.baskettecase.embedProc.service;

/**
 * Enum representing different document types based on their source path
 */
public enum DocumentType {
    POLICY("policy"),
    REFERENCE("reference"),
    INFORMATION("information"),
    UNKNOWN("unknown");
    
    private final String value;
    
    DocumentType(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    /**
     * Determine document type from file URL/path and filename
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
        } else if (lowerUrl.contains("/processed_files")) {
            // Extract filename from URL to check pattern
            String filename = extractFilename(fileUrl);
            if (filename != null && matchesRefnumPattern(filename)) {
                return POLICY; // Has refnum pattern, treat as policy
            } else {
                return INFORMATION; // No refnum pattern, treat as information
            }
        } else {
            return UNKNOWN;
        }
    }
    
    /**
     * Extract filename from URL
     */
    private static String extractFilename(String fileUrl) {
        if (fileUrl == null) return null;
        
        // Remove query parameters
        String urlWithoutQuery = fileUrl.split("\\?")[0];
        
        // Extract filename from path
        int lastSlash = urlWithoutQuery.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash < urlWithoutQuery.length() - 1) {
            return urlWithoutQuery.substring(lastSlash + 1);
        }
        return null;
    }
    
    /**
     * Check if filename matches the refnum pattern: <refnum1>-<refnum2>.*
     */
    private static boolean matchesRefnumPattern(String filename) {
        if (filename == null) return false;
        
        // Pattern: 6 digits, dash, 6 digits, then anything
        return filename.matches("^\\d{6}-\\d{6}\\..+");
    }
}
