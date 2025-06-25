package com.baskettecase.embedProc;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.ai.vectorstore.VectorStore;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class EmbedProcApplicationTests {

    @MockBean
    private VectorStore vectorStore;

    @Test
    void contextLoads() {
        // This test verifies that the Spring context loads successfully
        // VectorStore is mocked to avoid database dependencies in tests
    }
} 