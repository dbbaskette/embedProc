package com.baskettecase.embedProc.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RabbitMQMetricsPublisherTest {

    @Test
    void publishesToPipelineMetricsQueueWithMetaServiceEmbedProc() throws Exception {
        AmqpTemplate amqpTemplate = mock(AmqpTemplate.class);
        ObjectMapper objectMapper = new ObjectMapper();
        String expectedQueue = "pipeline.metrics";

        RabbitMQMetricsPublisher publisher = new RabbitMQMetricsPublisher(amqpTemplate, objectMapper, expectedQueue);

        MonitorService.MonitoringData data = new MonitorService.MonitoringData(
                "embedProc-0",
                "2025-01-01T00:00:00",
                1,
                1,
                0,
                1.0,
                "0h 1m",
                "IDLE",
                null,
                0,
                0,
                null,
                100,
                0,
                new MonitorService.MonitoringData.Meta("embedProc", null, null, null, null)
        );

        publisher.publishMetrics(data);

        ArgumentCaptor<String> queueCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);

        verify(amqpTemplate).send(queueCaptor.capture(), messageCaptor.capture());

        assertThat(queueCaptor.getValue()).isEqualTo(expectedQueue);

        byte[] body = messageCaptor.getValue().getBody();
        JsonNode json = objectMapper.readTree(body);

        // Existing fields intact
        assertThat(json.get("instanceId").asText()).isEqualTo("embedProc-0");
        assertThat(json.get("totalChunks").asLong()).isEqualTo(1L);
        assertThat(json.get("processedChunks").asLong()).isEqualTo(1L);
        assertThat(json.get("errorCount").asLong()).isEqualTo(0L);
        assertThat(json.get("processingRate").asDouble()).isEqualTo(1.0);
        assertThat(json.get("uptime").asText()).isEqualTo("0h 1m");
        assertThat(json.get("status").asText()).isEqualTo("IDLE");
        assertThat(json.get("memoryUsedMB").asLong()).isEqualTo(100L);
        assertThat(json.get("pendingMessages").asLong()).isEqualTo(0L);

        // New meta field
        assertThat(json.has("meta")).isTrue();
        assertThat(json.get("meta").get("service").asText()).isEqualTo("embedProc");
    }
}


