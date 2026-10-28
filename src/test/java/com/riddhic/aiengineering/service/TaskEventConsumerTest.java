package com.riddhic.aiengineering.service;

import com.riddhic.aiengineering.model.TaskEvent;
import com.riddhic.aiengineering.model.TaskEventType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class TaskEventConsumerTest {
    private final TaskEventConsumer consumer = new TaskEventConsumer();

    @Test
    void repeatedEventsAreSafeBecauseConsumerHasNoSideEffects() {
        TaskEvent event = new TaskEvent(TaskEventType.CREATED, 17L, "Read only event");
        assertDoesNotThrow(() -> {
            consumer.handleTaskEvent(event);
            consumer.handleTaskEvent(event);
        });
    }

    @Test
    void malformedNullEventIsIgnored() {
        assertDoesNotThrow(() -> consumer.handleTaskEvent(null));
    }
}
