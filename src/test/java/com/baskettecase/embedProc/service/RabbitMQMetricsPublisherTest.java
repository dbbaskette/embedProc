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

        // Use reflection to avoid direct compile-time reference
        Class<?> clazz = Class.forName("com.baskettecase.embedProc.service.RabbitMQMetricsPublisher");
        Object publisher = clazz.getConstructor(AmqpTemplate.class, ObjectMapper.class, String.class)
                .newInstance(amqpTemplate, objectMapper, expectedQueue);

        MonitorService.MonitoringData data = new MonitorService.MonitoringData(
                "embedProc-0",
                "2025-01-01T00:00:00",
                1L,
                1L,
                0L,
                1.0,
                "0h 1m",
                "IDLE",
                null,
                0L,
                0L,
                null,
                100L,
                0L,
                new MonitorService.MonitoringData.Meta("embedProc", null, null, null, null),
                "ip-10-0-1-23.ec2.internal",
                "embedproc.example.com",
                "HEARTBEAT",
                null,
                "http://embedproc.example.com:8080",
                1723100000000L,
                "0.0.5"
        );

        ((MetricsPublisher) publisher).publishMetrics(data);

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

        // Host fields
        assertThat(json.get("hostname").asText()).isEqualTo("ip-10-0-1-23.ec2.internal");
        assertThat(json.get("publicHostname").asText()).isEqualTo("embedproc.example.com");

        // Event field present
        assertThat(json.get("event").asText()).isEqualTo("HEARTBEAT");
    }
}


