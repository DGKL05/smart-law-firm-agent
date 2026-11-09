package com.example.dgkl.appointment.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.dgkl.appointment.client.ScheduleClient;
import com.example.dgkl.appointment.dto.AppointmentBookingRequest;
import com.example.dgkl.appointment.dto.AppointmentUpdateRequest;
import com.example.dgkl.appointment.dto.LawyerSnapshotResponse;
import com.example.dgkl.appointment.dto.ScheduleSlotResponse;
import com.example.dgkl.appointment.entity.Appointment;
import com.example.dgkl.appointment.entity.Consultation;
import com.example.dgkl.appointment.entity.LegalCase;
import com.example.dgkl.appointment.mapper.AppointmentMapper;
import com.example.dgkl.appointment.mapper.ConsultationMapper;
import com.example.dgkl.appointment.mapper.LegalCaseMapper;
import com.example.dgkl.appointment.service.AppointmentPeers;
import com.example.dgkl.appointment.service.AppointmentService;
import com.example.dgkl.appointment.service.AppointmentStatus;
import com.example.dgkl.appointment.service.AppointmentStore;
import com.example.dgkl.appointment.service.AppointmentTimes;
import com.example.dgkl.appointment.service.AppointmentTransactions;
import com.example.dgkl.appointment.service.CaseAvailability;
import com.example.dgkl.common.exception.BusinessException;
import com.example.dgkl.common.response.ApiResponse;
import com.example.dgkl.common.response.PageResult;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.seata.spring.annotation.GlobalLock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentServiceImpl implements AppointmentService {
    private static final String CLOSED_CONSULTATION = "CLOSED";
    private static final String ACTIVE_CASE = "ACTIVE";
    private final AppointmentMapper appointmentMapper;
    private final ConsultationMapper consultationMapper;
    private final LegalCaseMapper legalCaseMapper;
    private final AppointmentPeers peers;
    private final ScheduleClient scheduleClient;
    private final CaseAvailability cases;
    private final AppointmentStore store;
    private final AppointmentTransactions transactions;

    @Override
    public PageResult<Appointment> page(Access access, Long userId, Long lawyerId, long pageNum, long pageSize) {
        Long effectiveUser = access == Access.USER ? peers.currentUserId() : userId;
        Long effectiveLawyer = access == Access.LAWYER ? peers.currentLawyerId() : lawyerId;
        Page<Appointment> page = appointmentMapper.selectPage(Page.of(pageNum, pageSize),
                Wrappers.<Appointment>lambdaQuery()
                        .eq(effectiveUser != null, Appointment::getUserId, effectiveUser)
                        .eq(effectiveLawyer != null, Appointment::getLawyerId, effectiveLawyer)
                        .orderByDesc(Appointment::getAppointmentTime, Appointment::getId));
        return new PageResult<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public Appointment detail(Access access, Long id) {
        return requireAccess(access, appointmentMapper.selectById(id));
    }

    @Override
    public Appointment book(Access access, AppointmentBookingRequest request) {
        LocalDateTime time = request.getAppointmentTime();
        if (!time.isAfter(AppointmentTimes.now()) || time.getSecond() != 0 || time.getNano() != 0) {
            throw new BusinessException("预约时间必须在未来且精确到分钟");
        }
        Long userId = bookingUser(access, request.getUserId());
        LawyerSnapshotResponse lawyer = peers.lawyer(request.getLawyerId());
        if (!Integer.valueOf(1).equals(lawyer.getStatus())) {
            throw new BusinessException("律师当前不可预约");
        }
        return detail(access, transactions.book(userId, lawyer, request));
    }

    @Override
    @GlobalLock
    @Transactional(rollbackFor = Exception.class)
    public Appointment update(Access access, Long id, AppointmentUpdateRequest request) {
        requireAccess(access, store.forUpdate(id));
        int updated = appointmentMapper.update(null, Wrappers.<Appointment>lambdaUpdate()
                .eq(Appointment::getId, id)
                .in(Appointment::getStatus, AppointmentStatus.BOOKED, AppointmentStatus.BOOKING)
                .set(Appointment::getRemark, request.remark()));
        if (updated != 1) {
            throw new BusinessException(409, "当前状态不可修改备注");
        }
        return detail(access, id);
    }

    @Override
    public Appointment cancel(Access access, Long id) {
        Appointment appointment = detail(access, id);
        transactions.cancel(id, appointment.getLawyerId());
        return detail(access, id);
    }

    @Override
    @GlobalLock
    @Transactional(rollbackFor = Exception.class)
    public void delete(Access access, Long id) {
        requireAccess(access, store.forUpdate(id));
        int deleted = appointmentMapper.delete(Wrappers.<Appointment>lambdaQuery()
                .eq(Appointment::getId, id)
                .in(Appointment::getStatus, AppointmentStatus.CANCELLED, AppointmentStatus.REJECTED));
        if (deleted != 1) {
            throw new BusinessException(409, "请先取消预约并等待处理完成");
        }
    }

    @Override
    public List<ScheduleSlotResponse> schedule(Long lawyerId) {
        List<ScheduleSlotResponse> slots = loadSlots(lawyerId);
        if (slots.isEmpty()) {
            return slots;
        }
        LocalDateTime start = slots.get(0).getStartTime();
        LocalDateTime end = slots.get(slots.size() - 1).getEndTime();
        List<LegalCase> overlapping = cases.overlapping(lawyerId, start, end);
        for (ScheduleSlotResponse slot : slots) {
            if (Boolean.TRUE.equals(slot.getAvailable()) && overlapping.stream().anyMatch(legalCase ->
                    legalCase.getStartTime().isBefore(slot.getEndTime())
                            && legalCase.getEndTime().isAfter(slot.getStartTime()))) {
                slot.setAvailable(false);
                slot.setUnavailableReason("案件占用");
            }
        }
        return slots;
    }

    @Override
    public boolean hasActiveWork(Long lawyerId) {
        return appointmentMapper.exists(Wrappers.<Appointment>lambdaQuery()
                .eq(Appointment::getLawyerId, lawyerId)
                .and(status -> status.in(Appointment::getStatus, AppointmentStatus.BOOKING,
                                AppointmentStatus.CANCELLING)
                        .or(booked -> booked.eq(Appointment::getStatus, AppointmentStatus.BOOKED)
                                .ge(Appointment::getAppointmentTime, AppointmentTimes.now()))))
                || consultationMapper.exists(Wrappers.<Consultation>lambdaQuery()
                        .eq(Consultation::getLawyerId, lawyerId)
                        .ne(Consultation::getStatus, CLOSED_CONSULTATION))
                || legalCaseMapper.exists(Wrappers.<LegalCase>lambdaQuery()
                        .eq(LegalCase::getLawyerId, lawyerId)
                        .eq(LegalCase::getStatus, ACTIVE_CASE));
    }

    @Override
    public long count() {
        return appointmentMapper.selectCount(Wrappers.<Appointment>lambdaQuery());
    }

    private Long bookingUser(Access access, Long requestedUser) {
        if (access == Access.ADMIN) {
            peers.requireUser(requestedUser);
            return requestedUser;
        }
        Long userId = peers.currentUserId();
        if (requestedUser != null && !userId.equals(requestedUser)) {
            throw new BusinessException(403, "只能为本人预约");
        }
        return userId;
    }

    private Appointment requireAccess(Access access, Appointment appointment) {
        if (appointment == null) {
            throw new BusinessException(404, "预约不存在");
        }
        boolean allowed = switch (access) {
            case ADMIN -> true;
            case USER -> peers.currentUserId().equals(appointment.getUserId());
            case LAWYER -> peers.currentLawyerId().equals(appointment.getLawyerId());
        };
        if (!allowed) {
            throw new BusinessException(404, "预约不存在");
        }
        return appointment;
    }

    private List<ScheduleSlotResponse> loadSlots(Long lawyerId) {
        try {
            ApiResponse<List<ScheduleSlotResponse>> response = scheduleClient.slots(lawyerId);
            if (response != null && response.getCode() == 200 && response.getData() != null) {
                return response.getData();
            }
        } catch (FeignException exception) {
            log.warn("Schedule lookup failed for lawyer {}: HTTP {}", lawyerId, exception.status());
        }
        throw new BusinessException(503, "暂时无法查询可预约时段");
    }
}
