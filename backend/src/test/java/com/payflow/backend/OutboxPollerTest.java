package com.payflow.backend;

import com.payflow.backend.entity.OutboxEvent;
import com.payflow.backend.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class OutboxPollerTest extends AbstractIntegrationTest {

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @BeforeEach
    void setUp() {
        outboxEventRepository.deleteAll();
    }

    @Test
    void shouldProcessPendingEvent() {
        // Given: Insert a PENDING outbox event manually
        OutboxEvent event = OutboxEvent.builder()
                .eventType("TEST_EVENT")
                .payload("{\"key\":\"value\"}")
                .status("PENDING")
                .build();
        event = outboxEventRepository.save(event);
        UUID eventId = event.getId();

        // When/Then: Wait 4 seconds (two poller cycles) and assert event status is now PROCESSED
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            Optional<OutboxEvent> processedEvent = outboxEventRepository.findById(eventId);
            assertThat(processedEvent).isPresent();
            assertThat(processedEvent.get().getStatus()).isEqualTo("PROCESSED");
            assertThat(processedEvent.get().getProcessedAt()).isNotNull();
        });
    }

    @Test
    void shouldIncrementRetryCountAndEventuallyFail_WhenProcessingFails() {
        // Given: Insert an event that triggers failure simulation
        OutboxEvent event = OutboxEvent.builder()
                .eventType("FAIL_SIMULATION")
                .payload("{\"key\":\"value\"}")
                .status("PENDING")
                .build();
        event = outboxEventRepository.save(event);
        UUID eventId = event.getId();

        // When/Then: Wait until it fails 3 times
        await().atMost(8, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).untilAsserted(() -> {
            Optional<OutboxEvent> processedEvent = outboxEventRepository.findById(eventId);
            assertThat(processedEvent).isPresent();
            assertThat(processedEvent.get().getStatus()).isEqualTo("FAILED");
            assertThat(processedEvent.get().getRetryCount()).isGreaterThanOrEqualTo(3);
            assertThat(processedEvent.get().getProcessedAt()).isNull();
        });
    }
}
