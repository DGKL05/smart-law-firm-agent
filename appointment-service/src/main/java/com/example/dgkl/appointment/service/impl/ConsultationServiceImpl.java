package com.example.dgkl.appointment.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.dgkl.appointment.dto.ConsultationReplyRequest;
import com.example.dgkl.appointment.dto.ConsultationRequest;
import com.example.dgkl.appointment.dto.NotificationEvent;
import com.example.dgkl.appointment.entity.Consultation;
import com.example.dgkl.appointment.entity.NotificationOutbox;
import com.example.dgkl.appointment.mapper.ConsultationMapper;
import com.example.dgkl.appointment.mapper.NotificationOutboxMapper;
import com.example.dgkl.appointment.service.AppointmentPeers;
import com.example.dgkl.appointment.service.AppointmentTimes;
import com.example.dgkl.appointment.service.ConsultationService;
import com.example.dgkl.common.exception.BusinessException;
import com.example.dgkl.common.response.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConsultationServiceImpl implements ConsultationService {
    private static final String OPEN = "OPEN";
    private static final String REPLIED = "REPLIED";
    private static final String CLOSED = "CLOSED";

    private final ConsultationMapper mapper;
    private final AppointmentPeers peers;
    private final NotificationOutboxMapper outboxMapper;

    @Override
    public PageResult<Consultation> page(Access access, Long userId, Long lawyerId, long pageNum, long pageSize) {
        Long effectiveUser = access == Access.USER ? peers.currentUserId() : userId;
        Long effectiveLawyer = access == Access.LAWYER ? peers.currentLawyerId() : lawyerId;
        Page<Consultation> page = mapper.selectPage(Page.of(pageNum, pageSize), Wrappers.<Consultation>lambdaQuery()
                .eq(effectiveUser != null, Consultation::getUserId, effectiveUser)
                .eq(effectiveLawyer != null, Consultation::getLawyerId, effectiveLawyer)
                .orderByDesc(Consultation::getCreateTime, Consultation::getId));
        return new PageResult<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public Consultation detail(Access access, Long id) {
        Consultation consultation = mapper.selectById(id);
        if (consultation == null || !canAccess(access, consultation)) {
            throw new BusinessException(404, "咨询不存在");
        }
        return consultation;
    }

    @Override
    public Consultation create(Access access, ConsultationRequest request) {
        Long userId = creatingUser(access, request.userId());
        Consultation consultation = new Consultation();
        consultation.setUserId(userId);
        consultation.setLawyerId(request.lawyerId());
        consultation.setLawFirmId(assignedFirm(request));
        consultation.setTitle(request.title().trim());
        consultation.setQuestion(request.question().trim());
        consultation.setStatus(OPEN);
        mapper.insert(consultation);
        return detail(access, consultation.getId());
    }

    @Override
    public Consultation update(Access access, Long id, ConsultationRequest request) {
        Consultation consultation = detail(access, id);
        if (request.userId() != null && !request.userId().equals(consultation.getUserId())) {
            throw new BusinessException("不可更换咨询所属用户");
        }
        Long lawFirmId = assignedFirm(request);
        int updated = mapper.update(null, Wrappers.<Consultation>lambdaUpdate()
                .eq(Consultation::getId, id)
                .eq(Consultation::getStatus, OPEN)
                .set(Consultation::getLawyerId, request.lawyerId())
                .set(Consultation::getLawFirmId, lawFirmId)
                .set(Consultation::getTitle, request.title().trim())
                .set(Consultation::getQuestion, request.question().trim()));
        if (updated != 1) {
            throw new BusinessException(409, "只有待回复的咨询可修改");
        }
        return detail(access, id);
    }

    @Override
    public void delete(Access access, Long id) {
        detail(access, id);
        int deleted = mapper.delete(Wrappers.<Consultation>lambdaQuery()
                .eq(Consultation::getId, id)
                .eq(access == Access.USER, Consultation::getStatus, OPEN));
        if (deleted != 1) {
            throw new BusinessException(409, "只有待回复的咨询可删除");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Consultation reply(Long id, ConsultationReplyRequest request) {
        Long lawyerId = peers.currentLawyerId();
        Consultation consultation = mapper.selectOne(Wrappers.<Consultation>lambdaQuery()
                .eq(Consultation::getId, id)
                .eq(Consultation::getLawyerId, lawyerId)
                .last("FOR UPDATE"));
        if (consultation == null) {
            throw new BusinessException(404, "咨询不存在");
        }
        boolean firstReply = OPEN.equals(consultation.getStatus());
        if (!firstReply && !REPLIED.equals(consultation.getStatus())) {
            throw new BusinessException(409, "当前咨询不可回复");
        }
        String reply = request.reply().trim();
        if (!firstReply && reply.equals(consultation.getReply())) {
            return consultation;
        }
        int updated = mapper.update(null, Wrappers.<Consultation>lambdaUpdate()
                .eq(Consultation::getId, id)
                .eq(Consultation::getLawyerId, lawyerId)
                .eq(Consultation::getStatus, consultation.getStatus())
                .set(Consultation::getReply, reply)
                .set(Consultation::getStatus, REPLIED));
        if (updated != 1) {
            throw new BusinessException(409, "当前咨询不可回复");
        }
        if (firstReply) {
            enqueueFirstReply(consultation);
        }
        return mapper.selectById(id);
    }

    @Override
    public Consultation close(Long id) {
        detail(Access.ADMIN, id);
        mapper.update(null, Wrappers.<Consultation>lambdaUpdate()
                .eq(Consultation::getId, id)
                .in(Consultation::getStatus, OPEN, REPLIED)
                .set(Consultation::getStatus, CLOSED));
        return detail(Access.ADMIN, id);
    }

    @Override
    public long count() {
        return mapper.selectCount(Wrappers.<Consultation>lambdaQuery());
    }

    private void enqueueFirstReply(Consultation consultation) {
        NotificationOutbox outbox = new NotificationOutbox();
        outbox.setEventKey(NotificationEvent.firstConsultationReplyKey(consultation.getId()));
        outbox.setUserId(consultation.getUserId());
        outbox.setTitle("咨询已回复");
        outbox.setContent("您的咨询 #%d 已有律师回复，请在我的咨询查看。".formatted(consultation.getId()));
        outbox.setNextAttemptTime(AppointmentTimes.now());
        outboxMapper.insert(outbox);
    }

    private Long creatingUser(Access access, Long requested) {
        if (access == Access.ADMIN) {
            peers.requireUser(requested);
            return requested;
        }
        Long userId = peers.currentUserId();
        if (requested != null && !userId.equals(requested)) {
            throw new BusinessException(403, "只能以本人身份咨询");
        }
        return userId;
    }

    private Long assignedFirm(ConsultationRequest request) {
        if (request.lawyerId() == null) {
            return request.lawFirmId();
        }
        Long lawFirmId = peers.lawyer(request.lawyerId()).getLawFirmId();
        if (request.lawFirmId() != null && !request.lawFirmId().equals(lawFirmId)) {
            throw new BusinessException("律师不属于指定律所");
        }
        return lawFirmId;
    }

    private boolean canAccess(Access access, Consultation consultation) {
        return switch (access) {
            case ADMIN -> true;
            case USER -> peers.currentUserId().equals(consultation.getUserId());
            case LAWYER -> peers.currentLawyerId().equals(consultation.getLawyerId());
        };
    }
}
