package com.example.dgkl.notification.messaging;

import com.example.dgkl.notification.dto.NotificationDeliveryRequest;
import com.example.dgkl.notification.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Message;
import org.springframework.dao.DataAccessResourceFailureException;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class NotificationMessageListenerTest {
    private static final ValidatorFactory VALIDATORS = Validation.buildDefaultValidatorFactory();
    private static final String EVENT = """
            {"eventKey":"appointment:51:booked","userId":7,"title":"预约成功","content":"已预约咨询"}
            """;

    private final NotificationService service = mock(NotificationService.class);
    private final NotificationMessageListener listener = new NotificationMessageListener(service,
            new ObjectMapper(), VALIDATORS.getValidator());

    @AfterAll
    static void closeValidatorFactory() {
        VALIDATORS.close();
    }

    @Test
    void acceptsProducerEventWithOriginalIdempotencyKey() {
        listener.receiveAppointment(message(EVENT));

        verify(service).deliver(new NotificationDeliveryRequest("appointment:51:booked", 7L, "预约成功", "已预约咨询"));
    }

    @Test
    void rejectsEventWithoutRecipientBeforeWriting() {
        assertThatThrownBy(() -> listener.receiveAppointment(message(EVENT.replace("\"userId\":7,", ""))))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);
        verifyNoInteractions(service);
    }

    @Test
    void databaseFailureRemainsVisibleToRabbitRetryPolicy() {
        doThrow(new DataAccessResourceFailureException("MySQL unavailable")).when(service)
                .deliver(new NotificationDeliveryRequest("appointment:51:booked", 7L, "预约成功", "已预约咨询"));

        assertThatThrownBy(() -> listener.receiveAppointment(message(EVENT)))
                .isInstanceOf(DataAccessResourceFailureException.class);
    }

    @Test
    void acceptsConsultationReplyWithItsEventKey() {
        listener.receiveConsultation(message("""
                {"eventKey":"consultation:41:first-reply","userId":7,"title":"咨询已回复","content":"请查看我的咨询"}
                """));

        verify(service).deliver(new NotificationDeliveryRequest("consultation:41:first-reply", 7L,
                "咨询已回复", "请查看我的咨询"));
    }

    @Test
    void bothLanesHaveDistinctDeadLetterQueues() {
        var topology = new NotificationQueueConfig().notificationTopology();

        assertThat(topology.getDeclarablesByType(Binding.class))
                .extracting(Binding::getRoutingKey, Binding::getDestination)
                .containsExactlyInAnyOrder(
                        tuple("appointment.notification", "smart-law.notifications.appointment"),
                        tuple("appointment.notification.failed", "smart-law.notifications.appointment.dlq"),
                        tuple("consultation.notification", "smart-law.notifications.consultation"),
                        tuple("consultation.notification.failed", "smart-law.notifications.consultation.dlq"));
    }

    private Message message(String value) {
        return new Message(value.getBytes(StandardCharsets.UTF_8));
    }
}
