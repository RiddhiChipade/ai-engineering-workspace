package com.riddhic.aiengineering.service;

import com.riddhic.aiengineering.model.TaskEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.kafka.support.SendResult;

@Service
public class TaskEventProducer {

    private static final Logger log = LoggerFactory.getLogger(TaskEventProducer.class);

    private final KafkaTemplate<String, TaskEvent> kafkaTemplate;
    private final String topic;

    public TaskEventProducer(KafkaTemplate<String, TaskEvent> kafkaTemplate,
                              @Value("${kafka.task.topic:task-events}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void publishTaskEvent(TaskEvent event) {
        String key = event.getTaskId() != null ? String.valueOf(event.getTaskId()) : null;
        log.info("Publishing task event key={} type={} eventId={}", key, event.getEventType(), event.getEventId());
        kafkaTemplate.send(topic, key, event).whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish task event eventId={}", event.getEventId(), ex);
                    } else {
                        try {
                            log.info("Published task event to topic={}, partition={}, offset={}",
                                    result.getRecordMetadata().topic(), result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
                        } catch (Exception e) {
                            log.info("Published task event but could not read record metadata: {}", e.getMessage());
                        }
            }
        });
    }
}
