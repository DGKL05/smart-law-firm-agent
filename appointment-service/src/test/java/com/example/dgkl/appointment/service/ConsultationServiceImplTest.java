package com.example.dgkl.appointment.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.example.dgkl.appointment.dto.ConsultationReplyRequest;
import com.example.dgkl.appointment.entity.Consultation;
import com.example.dgkl.appointment.entity.NotificationOutbox;
import com.example.dgkl.appointment.mapper.ConsultationMapper;
import com.example.dgkl.appointment.mapper.NotificationOutboxMapper;
import com.example.dgkl.appointment.service.impl.ConsultationServiceImpl;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ConsultationServiceImplTest {
    private static final long CONSULTATION_ID = 41L;
    private static final long USER_ID = 7L;
    private static final long LAWYER_ID = 8L;

    private final ConsultationMapper mapper = mock(ConsultationMapper.class);
    private final AppointmentPeers peers = mock(AppointmentPeers.class);
    private final NotificationOutboxMapper outbox = mock(NotificationOutboxMapper.class);
    private final ConsultationService service = new ConsultationServiceImpl(mapper, peers, outbox);

    @BeforeAll
    static void initializeMapperMetadata() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "consultation-test"),
                Consultation.class);
    }

    @Test
    void firstReplyEnqueuesOneNoticeForConsultationOwner() {
        when(peers.currentLawyerId()).thenReturn(LAWYER_ID);
        when(mapper.selectOne(any())).thenReturn(consultation("OPEN", null));
        when(mapper.update(isNull(), any())).thenReturn(1);
        when(mapper.selectById(CONSULTATION_ID)).thenReturn(consultation("REPLIED", "律师回复正文"));

        Consultation replied = service.reply(CONSULTATION_ID, new ConsultationReplyRequest("律师回复正文"));

        var event = ArgumentCaptor.forClass(NotificationOutbox.class);
        verify(outbox).insert(event.capture());
        assertThat(event.getValue().getEventKey()).isEqualTo("consultation:41:first-reply");
        assertThat(event.getValue().getUserId()).isEqualTo(USER_ID);
        assertThat(event.getValue().getContent()).doesNotContain("律师回复正文");
        assertThat(replied.getStatus()).isEqualTo("REPLIED");
    }

    @Test
    void editingAndRetryingAReplyDoNotEnqueueAnotherNotice() {
        when(peers.currentLawyerId()).thenReturn(LAWYER_ID);
        when(mapper.selectOne(any())).thenReturn(consultation("REPLIED", "首次回复"),
                consultation("REPLIED", "更新后的回复"));
        when(mapper.update(isNull(), any())).thenReturn(1);
        when(mapper.selectById(CONSULTATION_ID)).thenReturn(consultation("REPLIED", "更新后的回复"));

        var edited = new ConsultationReplyRequest("更新后的回复");
        assertThat(service.reply(CONSULTATION_ID, edited).getReply()).isEqualTo("更新后的回复");
        assertThat(service.reply(CONSULTATION_ID, edited).getReply()).isEqualTo("更新后的回复");

        verify(mapper).update(isNull(), any());
        verifyNoInteractions(outbox);
    }

    private Consultation consultation(String status, String reply) {
        var record = new Consultation();
        record.setId(CONSULTATION_ID);
        record.setUserId(USER_ID);
        record.setLawyerId(LAWYER_ID);
        record.setStatus(status);
        record.setReply(reply);
        return record;
    }
}
