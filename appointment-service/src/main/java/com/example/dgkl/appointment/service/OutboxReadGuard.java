package com.example.dgkl.appointment.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.dgkl.appointment.entity.NotificationOutbox;
import com.example.dgkl.appointment.mapper.NotificationOutboxMapper;
import lombok.RequiredArgsConstructor;
import org.apache.seata.spring.annotation.GlobalLock;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class OutboxReadGuard {
    private final NotificationOutboxMapper mapper;

    @GlobalLock
    @Transactional(rollbackFor = Exception.class)
    public NotificationOutbox claim(String eventKey, LocalDateTime now, LocalDateTime retryAt) {
        // The AT branch may be locally committed. Check its global lock before leasing the event.
        NotificationOutbox event = mapper.selectOne(Wrappers.<NotificationOutbox>lambdaQuery()
                .eq(NotificationOutbox::getEventKey, eventKey)
                .isNull(NotificationOutbox::getSentTime)
                .le(NotificationOutbox::getNextAttemptTime, now)
                .last("FOR UPDATE"));
        if (event == null) {
            return null;
        }
        mapper.update(null, Wrappers.<NotificationOutbox>lambdaUpdate()
                .eq(NotificationOutbox::getEventKey, eventKey)
                .set(NotificationOutbox::getNextAttemptTime, retryAt));
        event.setNextAttemptTime(retryAt);
        return event;
    }
}
