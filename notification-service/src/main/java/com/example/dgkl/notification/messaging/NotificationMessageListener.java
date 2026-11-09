package com.example.dgkl.notification.messaging;

import com.example.dgkl.notification.dto.NotificationDeliveryRequest;
import com.example.dgkl.notification.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class NotificationMessageListener {
    private static final int MAX_BODY_BYTES = 8192;

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    @RabbitListener(id = "appointment-notifications", queues = NotificationQueueConfig.APPOINTMENT_QUEUE)
    public void receiveAppointment(Message message) {
        notificationService.deliver(parse(message.getBody()));
    }

    @RabbitListener(id = "consultation-notifications", queues = NotificationQueueConfig.CONSULTATION_QUEUE)
    public void receiveConsultation(Message message) {
        notificationService.deliver(parse(message.getBody()));
    }

    private NotificationDeliveryRequest parse(byte[] body) {
        if (body.length == 0 || body.length > MAX_BODY_BYTES) {
            throw new AmqpRejectAndDontRequeueException("通知消息大小不符合约束");
        }
        NotificationDeliveryRequest event;
        try {
            event = objectMapper.readValue(body, NotificationDeliveryRequest.class);
        } catch (IOException exception) {
            throw new AmqpRejectAndDontRequeueException("通知消息不是有效 JSON");
        }
        if (event == null || !validator.validate(event).isEmpty()) {
            throw new AmqpRejectAndDontRequeueException("通知消息字段不符合约束");
        }
        return event;
    }
}
