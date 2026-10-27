package com.riddhic.aiengineering.service;

import com.riddhic.aiengineering.model.TaskEvent;
import com.riddhic.aiengineering.model.TaskEventType;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.util.concurrent.SettableListenableFuture;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TaskEventProducerTest {

    @Test
    void publishTaskEvent_sendsToKafka() {
        KafkaTemplate<String, TaskEvent> kafkaTemplate = Mockito.mock(KafkaTemplate.class);
        when(kafkaTemplate.send(eq("task-events"), eq("123"), Mockito.any(TaskEvent.class))).thenReturn(java.util.concurrent.CompletableFuture.completedFuture(null));

        TaskEventProducer producer = new TaskEventProducer(kafkaTemplate, "task-events");

        TaskEvent event = new TaskEvent(TaskEventType.CREATED, 123L, "Test task");
        producer.publishTaskEvent(event);

        verify(kafkaTemplate).send(eq("task-events"), eq("123"), Mockito.any(TaskEvent.class));
    }
}