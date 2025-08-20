package com.baskettecase.embedProc.service;

import java.util.Map;
import java.util.HashMap;

/**
 * Demonstration class showing how the new metadata system works
 * This is not a test, just a demo class to show the functionality
 */
public class MetadataDemo {

    public static void main(String[] args) {
        System.out.println("=== EmbedProc Metadata System Demonstration ===\n");
        
        // Demonstrate document type detection
        demonstrateDocumentTypeDetection();
        
        // Demonstrate metadata creation
        demonstrateMetadataCreation();
        
        System.out.println("\n=== End of Demonstration ===");
    }
    
    private static void demonstrateDocumentTypeDetection() {
        System.out.println("1. Document Type Detection:");
        System.out.println("   URL: http://example.com/policies/contract.txt");
        System.out.println("   Type: " + DocumentType.fromUrl("http://example.com/policies/contract.txt"));
        
        System.out.println("   URL: http://example.com/reference/manual.txt");
        System.out.println("   Type: " + DocumentType.fromUrl("http://example.com/reference/manual.txt"));
        
        System.out.println("   URL: http://example.com/documents/file.txt");
        System.out.println("   Type: " + DocumentType.fromUrl("http://example.com/documents/file.txt"));
        System.out.println();
    }
    
    private static void demonstrateMetadataCreation() {
        System.out.println("2. Metadata Creation Examples:");
        
        // Policy document metadata
        Map<String, Object> policyMetadata = createMetadata(100001, 200001, DocumentType.POLICY, "http://example.com/policies/contract.txt");
        System.out.println("   Policy Document:");
        System.out.println("     - refnum1: " + policyMetadata.get("refnum1"));
        System.out.println("     - refnum2: " + policyMetadata.get("refnum2"));
        System.out.println("     - doctype: " + policyMetadata.get("doctype"));
        System.out.println("     - sourcePath: " + policyMetadata.get("sourcePath"));
        System.out.println();
        
        // Reference document metadata
        Map<String, Object> referenceMetadata = createMetadata(null, null, DocumentType.REFERENCE, "http://example.com/reference/manual.txt");
        System.out.println("   Reference Document:");
        System.out.println("     - refnum1: " + referenceMetadata.get("refnum1"));
        System.out.println("     - refnum2: " + referenceMetadata.get("refnum2"));
        System.out.println("     - doctype: " + referenceMetadata.get("doctype"));
        System.out.println("     - sourcePath: " + referenceMetadata.get("sourcePath"));
        System.out.println();
        
        // Unknown document metadata
        Map<String, Object> unknownMetadata = createMetadata(500001, 600001, DocumentType.UNKNOWN, "http://example.com/documents/file.txt");
        System.out.println("   Unknown Document:");
        System.out.println("     - refnum1: " + unknownMetadata.get("refnum1"));
        System.out.println("     - refnum2: " + unknownMetadata.get("refnum2"));
        System.out.println("     - documentType: " + unknownMetadata.get("documentType"));
        System.out.println("     - sourcePath: " + unknownMetadata.get("sourcePath"));
        System.out.println();
    }
    
    private static Map<String, Object> createMetadata(Integer refnum1, Integer refnum2, DocumentType documentType, String sourcePath) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("timestamp", System.currentTimeMillis());
        
        // Add document type specific metadata
        switch (documentType) {
            case POLICY:
                // For policies, include reference numbers
                metadata.put("refnum1", refnum1);
                metadata.put("refnum2", refnum2);
                break;
            case REFERENCE:
                // For reference documents, add doctype=information (no refnums)
                metadata.put("doctype", "information");
                break;
            case UNKNOWN:
            default:
                // For unknown types, add the document type for debugging
                metadata.put("documentType", documentType.getValue());
                // Include refnums if they exist (for backward compatibility)
                if (refnum1 != null) {
                    metadata.put("refnum1", refnum1);
                }
                if (refnum2 != null) {
                    metadata.put("refnum2", refnum2);
                }
                break;
        }
        
        // Add source path if available
        if (sourcePath != null) {
            metadata.put("sourcePath", sourcePath);
        }
        
        return metadata;
    }
}
