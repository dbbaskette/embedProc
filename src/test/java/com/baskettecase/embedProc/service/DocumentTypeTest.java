package com.baskettecase.embedProc.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for DocumentType enum functionality
 */
public class DocumentTypeTest {

    @Test
    public void testPolicyDocumentType() {
        // Test URLs containing /policies
        assertEquals(DocumentType.POLICY, DocumentType.fromUrl("http://example.com/policies/document.txt"));
        assertEquals(DocumentType.POLICY, DocumentType.fromUrl("https://api.company.com/api/v1/policies/123.txt"));
        assertEquals(DocumentType.POLICY, DocumentType.fromUrl("/policies/important-doc.pdf.txt"));
        assertEquals(DocumentType.POLICY, DocumentType.fromUrl("file:///home/user/policies/contract.txt"));
    }

    @Test
    public void testReferenceDocumentType() {
        // Test URLs containing /reference
        assertEquals(DocumentType.REFERENCE, DocumentType.fromUrl("http://example.com/reference/manual.txt"));
        assertEquals(DocumentType.REFERENCE, DocumentType.fromUrl("https://docs.company.com/reference/api.txt"));
        assertEquals(DocumentType.REFERENCE, DocumentType.fromUrl("/reference/guide.pdf.txt"));
        assertEquals(DocumentType.REFERENCE, DocumentType.fromUrl("file:///home/user/reference/readme.txt"));
    }

    @Test
    public void testInformationDocumentType() {
        // Test URLs containing /processed_files with non-refnum filenames
        assertEquals(DocumentType.INFORMATION, DocumentType.fromUrl("http://big-data-005.kuhn-labs.com:9870/webhdfs/v1/processed_files/glossary.pdf.txt?op=OPEN"));
        assertEquals(DocumentType.INFORMATION, DocumentType.fromUrl("https://hdfs.company.com/processed_files/manual.txt"));
        assertEquals(DocumentType.INFORMATION, DocumentType.fromUrl("/processed_files/guide.pdf.txt"));
        assertEquals(DocumentType.INFORMATION, DocumentType.fromUrl("file:///home/user/processed_files/readme.txt"));
    }

    @Test
    public void testProcessedFilesPolicyDocumentType() {
        // Test URLs containing /processed_files with refnum pattern filenames
        assertEquals(DocumentType.POLICY, DocumentType.fromUrl("http://big-data-005.kuhn-labs.com:9870/webhdfs/v1/processed_files/123456-789012.pdf.txt?op=OPEN"));
        assertEquals(DocumentType.POLICY, DocumentType.fromUrl("https://hdfs.company.com/processed_files/100001-200001.doc.txt"));
        assertEquals(DocumentType.POLICY, DocumentType.fromUrl("/processed_files/555555-666666.pdf.txt"));
        assertEquals(DocumentType.POLICY, DocumentType.fromUrl("file:///home/user/processed_files/111111-222222.docx.txt"));
    }

    @Test
    public void testUnknownDocumentType() {
        // Test URLs that don't contain /policies, /reference, or /processed_files
        assertEquals(DocumentType.UNKNOWN, DocumentType.fromUrl("http://example.com/documents/file.txt"));
        assertEquals(DocumentType.UNKNOWN, DocumentType.fromUrl("https://api.company.com/files/data.txt"));
        assertEquals(DocumentType.UNKNOWN, DocumentType.fromUrl("/uploads/document.txt"));
        assertEquals(DocumentType.UNKNOWN, DocumentType.fromUrl("file:///home/user/downloads/file.txt"));
        assertEquals(DocumentType.UNKNOWN, DocumentType.fromUrl(""));
        assertEquals(DocumentType.UNKNOWN, DocumentType.fromUrl(null));
    }

    @Test
    public void testCaseInsensitive() {
        // Test that the matching is case insensitive
        assertEquals(DocumentType.POLICY, DocumentType.fromUrl("http://example.com/POLICIES/document.txt"));
        assertEquals(DocumentType.POLICY, DocumentType.fromUrl("http://example.com/Policies/document.txt"));
        assertEquals(DocumentType.REFERENCE, DocumentType.fromUrl("http://example.com/REFERENCE/document.txt"));
        assertEquals(DocumentType.REFERENCE, DocumentType.fromUrl("http://example.com/Reference/document.txt"));
        assertEquals(DocumentType.INFORMATION, DocumentType.fromUrl("http://example.com/PROCESSED_FILES/document.txt"));
        assertEquals(DocumentType.INFORMATION, DocumentType.fromUrl("http://example.com/Processed_Files/document.txt"));
    }

    @Test
    public void testGetValue() {
        // Test that getValue() returns the correct string representation
        assertEquals("policy", DocumentType.POLICY.getValue());
        assertEquals("reference", DocumentType.REFERENCE.getValue());
        assertEquals("information", DocumentType.INFORMATION.getValue());
        assertEquals("unknown", DocumentType.UNKNOWN.getValue());
    }

    @Test
    public void testRefnumPatternMatching() {
        // Test filename pattern matching for processed_files
        // Files with refnum pattern should be POLICY
        assertEquals(DocumentType.POLICY, DocumentType.fromUrl("/processed_files/123456-789012.pdf.txt"));
        assertEquals(DocumentType.POLICY, DocumentType.fromUrl("/processed_files/100001-200001.docx.txt"));
        
        // Files without refnum pattern should be INFORMATION
        assertEquals(DocumentType.INFORMATION, DocumentType.fromUrl("/processed_files/glossary.pdf.txt"));
        assertEquals(DocumentType.INFORMATION, DocumentType.fromUrl("/processed_files/manual-guide.txt"));
        assertEquals(DocumentType.INFORMATION, DocumentType.fromUrl("/processed_files/123-456.txt")); // Too few digits
        assertEquals(DocumentType.INFORMATION, DocumentType.fromUrl("/processed_files/1234567-789012.txt")); // Too many digits
    }
}
