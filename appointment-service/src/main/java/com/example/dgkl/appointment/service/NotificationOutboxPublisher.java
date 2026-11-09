package com.example.dgkl.appointment.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.dgkl.appointment.dto.NotificationEvent;
import com.example.dgkl.appointment.entity.NotificationOutbox;
import com.example.dgkl.appointment.mapper.NotificationOutboxMapper;
import com.example.dgkl.appointment.messaging.NotificationMessagePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationOutboxPublisher {
    private static final int RETRY_SECONDS = 30;

    private final NotificationOutboxMapper mapper;
    private final OutboxReadGuard outboxReadGuard;
    private final NotificationMessagePublisher messagePublisher;

    @Scheduled(fixedDelayString = "${appointment.notification-delay-ms:5000}")
    public void publishDue() {
        List<NotificationOutbox> due = mapper.selectList(Wrappers.<NotificationOutbox>lambdaQuery()
                .isNull(NotificationOutbox::getSentTime)
                .le(NotificationOutbox::getNextAttemptTime, AppointmentTimes.now())
                .orderByAsc(NotificationOutbox::getNextAttemptTime)
                .last("LIMIT 20"));
        for (NotificationOutbox message : due) {
            if (Thread.currentThread().isInterrupted()) {
                break;
            }
            publish(message);
        }
    }

    private void publish(NotificationOutbox message) {
        LocalDateTime now = AppointmentTimes.now();
        NotificationOutbox claimed;
        try {
            claimed = outboxReadGuard.claim(message.getEventKey(), now, now.plusSeconds(RETRY_SECONDS));
        } catch (RuntimeException exception) {
            log.debug("暂不能领取通知事件，eventKey={}，error={}", message.getEventKey(),
                    exception.getClass().getSimpleName());
            return;
        }
        if (claimed == null) {
            return;
        }
        NotificationEvent event = new NotificationEvent(claimed.getEventKey(), claimed.getUserId(),
                claimed.getTitle(), claimed.getContent());
        if (messagePublisher.publish(event)) {
            mapper.update(null, Wrappers.<NotificationOutbox>lambdaUpdate()
                    .eq(NotificationOutbox::getEventKey, claimed.getEventKey())
                    .isNull(NotificationOutbox::getSentTime)
                    .set(NotificationOutbox::getSentTime, AppointmentTimes.now()));
        }
    }
}
