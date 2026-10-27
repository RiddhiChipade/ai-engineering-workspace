package com.riddhic.aiengineering.service;

import com.riddhic.aiengineering.model.TaskEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Service
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = false)
public class TaskEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(TaskEventConsumer.class);

    @KafkaListener(topics = "${kafka.task.topic:task-events}", groupId = "${KAFKA_CONSUMER_GROUP:ai-engineering-workspace}")
    public void handleTaskEvent(TaskEvent event) {
        if (event == null) {
            log.warn("Received null or malformed task event");
            return;
        }

            try {
                log.info("Consumed task event: eventId={} type={} taskId={} title={} timestamp={}",
                        event.getEventId(), event.getEventType(), event.getTaskId(), event.getTitle(), event.getTimestamp());
            } catch (Exception ex) {
                log.error("Error handling consumed task event", ex);
                // rethrow to let container handle retries if configured
                throw ex;
            }
        }
}
