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
    public void testUnknownDocumentType() {
        // Test URLs that don't contain /policies or /reference
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
    }

    @Test
    public void testGetValue() {
        // Test that getValue() returns the correct string representation
        assertEquals("policy", DocumentType.POLICY.getValue());
        assertEquals("reference", DocumentType.REFERENCE.getValue());
        assertEquals("unknown", DocumentType.UNKNOWN.getValue());
    }
}
