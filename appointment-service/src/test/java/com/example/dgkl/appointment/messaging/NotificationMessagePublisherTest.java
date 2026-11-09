package com.example.dgkl.appointment.messaging;

import com.example.dgkl.appointment.dto.NotificationEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

class NotificationMessagePublisherTest {
    private static final NotificationEvent EVENT = new NotificationEvent(
            "appointment:51:booked", 7L, "预约成功", "已预约咨询");

    private final RabbitTemplate template = mock(RabbitTemplate.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final NotificationMessagePublisher publisher = new NotificationMessagePublisher(template, objectMapper, 20);

    @Test
    void onlyTreatsRoutedPositiveConfirmAsSent() {
        doAnswer(invocation -> {
            Message message = invocation.getArgument(2);
            CorrelationData correlation = invocation.getArgument(3);
            assertThat(message.getMessageProperties().getDeliveryMode()).isEqualTo(MessageDeliveryMode.PERSISTENT);
            assertThat(objectMapper.readValue(message.getBody(), NotificationEvent.class)).isEqualTo(EVENT);
            correlation.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(template).send(eq("smart-law.notifications"), eq("appointment.notification"),
                any(Message.class), any(CorrelationData.class));

        assertThat(publisher.publish(EVENT)).isTrue();
    }

    @Test
    void rejectsUnroutableMessageEvenWithPositiveConfirm() {
        doAnswer(invocation -> {
            Message message = invocation.getArgument(2);
            CorrelationData correlation = invocation.getArgument(3);
            correlation.setReturned(new ReturnedMessage(message, 312, "NO_ROUTE",
                    "smart-law.notifications", "appointment.notification"));
            correlation.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(template).send(eq("smart-law.notifications"), eq("appointment.notification"),
                any(Message.class), any(CorrelationData.class));

        assertThat(publisher.publish(EVENT)).isFalse();
    }

    @Test
    void routesFirstConsultationReplyToItsOwnQueue() {
        var event = new NotificationEvent(NotificationEvent.firstConsultationReplyKey(51L), 7L,
                "咨询已回复", "请查看我的咨询");
        doAnswer(invocation -> {
            Message message = invocation.getArgument(2);
            CorrelationData correlation = invocation.getArgument(3);
            assertThat(objectMapper.readValue(message.getBody(), NotificationEvent.class)).isEqualTo(event);
            correlation.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(template).send(eq("smart-law.notifications"), eq("consultation.notification"),
                any(Message.class), any(CorrelationData.class));

        assertThat(publisher.publish(event)).isTrue();
    }

    @Test
    void leavesUnconfirmedMessageForRetry() {
        assertThat(publisher.publish(EVENT)).isFalse();
    }
}
