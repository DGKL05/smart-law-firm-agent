package com.example.dgkl.appointment.messaging;

import com.example.dgkl.appointment.dto.NotificationEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
public class NotificationMessagePublisher {
    static final String EXCHANGE = "smart-law.notifications";
    static final String APPOINTMENT_ROUTING_KEY = "appointment.notification";
    static final String CONSULTATION_ROUTING_KEY = "consultation.notification";

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final long confirmTimeoutMillis;

    public NotificationMessagePublisher(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper,
                                        @Value("${appointment.notification-confirm-timeout-ms:5000}")
                                        long confirmTimeoutMillis) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.confirmTimeoutMillis = confirmTimeoutMillis;
    }

    public boolean publish(NotificationEvent event) {
        CorrelationData correlation = new CorrelationData(UUID.randomUUID().toString());
        try {
            String routingKey = event.eventKey().startsWith(NotificationEvent.CONSULTATION_KEY_PREFIX)
                    ? CONSULTATION_ROUTING_KEY : APPOINTMENT_ROUTING_KEY;
            rabbitTemplate.send(EXCHANGE, routingKey, message(event), correlation);
            CorrelationData.Confirm confirm = correlation.getFuture().get(confirmTimeoutMillis, TimeUnit.MILLISECONDS);
            // Spring AMQP populates the mandatory return on the correlation before completing its confirm future.
            if (confirm.isAck() && correlation.getReturned() == null) {
                return true;
            }
            log.warn("RabbitMQ 未接收站内通知，eventKey={}，ack={}，returned={}", event.eventKey(),
                    confirm.isAck(), correlation.getReturned() != null);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } catch (AmqpException | JsonProcessingException | ExecutionException | TimeoutException exception) {
            log.warn("RabbitMQ 站内通知发布失败，eventKey={}，error={}", event.eventKey(),
                    exception.getClass().getSimpleName());
        }
        return false;
    }

    private Message message(NotificationEvent event) throws JsonProcessingException {
        MessageProperties properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        properties.setContentEncoding(StandardCharsets.UTF_8.name());
        properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        properties.setMessageId(event.eventKey());
        return new Message(objectMapper.writeValueAsBytes(event), properties);
    }
}
