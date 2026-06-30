package com.example.dgkl.module.appointment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.dgkl.common.BusinessException;
import com.example.dgkl.common.PageResult;
import com.example.dgkl.module.appointment.dto.AppointmentBookingRequest;
import com.example.dgkl.module.appointment.entity.Appointment;
import com.example.dgkl.module.appointment.mapper.AppointmentMapper;
import com.example.dgkl.module.appointment.service.AppointmentScheduleService;
import com.example.dgkl.module.appointment.service.AppointmentStatusPolicy;
import com.example.dgkl.module.appointment.service.AppointmentService;
import com.example.dgkl.module.common.EntityLifecycle;
import com.example.dgkl.module.lawfirm.entity.LawFirm;
import com.example.dgkl.module.lawfirm.service.LawFirmService;
import com.example.dgkl.module.lawyer.entity.Lawyer;
import com.example.dgkl.module.lawyer.service.LawyerService;
import com.example.dgkl.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AppointmentServiceImpl extends ServiceImpl<AppointmentMapper, Appointment> implements AppointmentService {
    private final ObjectProvider<AppointmentScheduleService> appointmentScheduleServiceProvider;
    private final LawyerService lawyerService;
    private final LawFirmService lawFirmService;

    @Override
    public PageResult<Appointment> adminPage(long pageNum, long pageSize, String keyword) {
        completeExpiredAppointments();
        return PageResult.of(page(new Page<>(pageNum, pageSize), appointmentQuery(null, keyword)));
    }

    @Override
    public PageResult<Appointment> currentUserPage(long pageNum, long pageSize, String keyword) {
        Long userId = requireUserId();
        completeExpiredAppointmentsForUser(userId);
        return PageResult.of(page(new Page<>(pageNum, pageSize), appointmentQuery(userId, keyword)));
    }

    @Override
    public Appointment bookForCurrentUser(AppointmentBookingRequest request) {
        if (request == null || request.getLawyerId() == null || request.getAppointmentTime() == null) {
            throw new BusinessException("请选择律师和预约时间");
        }
        Lawyer lawyer = lawyerService.getById(request.getLawyerId());
        if (lawyer == null || lawyer.getStatus() == null || lawyer.getStatus() != 1) {
            throw new BusinessException("律师不存在或不可预约");
        }
        if (!appointmentScheduleServiceProvider.getObject().isSlotAvailable(request.getLawyerId(), request.getAppointmentTime())) {
            throw new BusinessException("该时间段不可预约，请选择其他时间");
        }
        LawFirm lawFirm = lawFirmService.getById(lawyer.getLawFirmId());
        Appointment appointment = new Appointment();
        appointment.setUserId(requireUserId());
        appointment.setLawyerId(lawyer.getId());
        appointment.setLawFirmId(lawyer.getLawFirmId());
        appointment.setLawyerName(lawyer.getName());
        appointment.setLawFirmName(lawFirm == null ? null : lawFirm.getName());
        appointment.setAppointmentTime(request.getAppointmentTime());
        appointment.setRemark(request.getRemark());
        appointment.setStatus(AppointmentStatusPolicy.STATUS_BOOKED);
        EntityLifecycle.forCreate(appointment);
        save(appointment);
        return appointment;
    }

    @Override
    public Appointment createForCurrentUser(Appointment appointment) {
        AppointmentBookingRequest request = new AppointmentBookingRequest();
        request.setLawyerId(appointment.getLawyerId());
        request.setAppointmentTime(appointment.getAppointmentTime());
        request.setRemark(appointment.getRemark());
        return bookForCurrentUser(request);
    }

    @Override
    public Appointment cancelForCurrentUser(Long id) {
        Long userId = requireUserId();
        completeExpiredAppointmentsForUser(userId);
        Appointment appointment = requireOwnAppointment(userId, id);
        if (!AppointmentStatusPolicy.STATUS_BOOKED.equals(appointment.getStatus())) {
            throw new BusinessException("只有已预约且未结束的预约可以取消");
        }
        appointment.setStatus(AppointmentStatusPolicy.STATUS_CANCELLED);
        EntityLifecycle.forUpdate(appointment);
        updateById(appointment);
        return getById(id);
    }

    @Override
    public void completeExpiredAppointments() {
        completeExpiredAppointments(null);
    }

    @Override
    public void completeExpiredAppointmentsForUser(Long userId) {
        if (userId == null) {
            return;
        }
        completeExpiredAppointments(userId);
    }

    private void completeExpiredAppointments(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        QueryWrapper<Appointment> wrapper = new QueryWrapper<Appointment>()
                .eq("status", AppointmentStatusPolicy.STATUS_BOOKED)
                .le("appointment_time", now.minusHours(1));
        if (userId != null) {
            wrapper.eq("user_id", userId);
        }
        List<Appointment> expired = list(wrapper);
        if (expired.isEmpty()) {
            return;
        }
        expired.forEach(appointment -> {
            if (AppointmentStatusPolicy.isExpired(appointment.getAppointmentTime(), now)) {
                appointment.setStatus(AppointmentStatusPolicy.STATUS_COMPLETED);
                appointment.setUpdateTime(now);
            }
        });
        updateBatchById(expired);
    }

    private QueryWrapper<Appointment> appointmentQuery(Long userId, String keyword) {
        QueryWrapper<Appointment> wrapper = new QueryWrapper<Appointment>().orderByDesc("create_time");
        if (userId != null) {
            wrapper.eq("user_id", userId);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(item -> item.like("law_firm_name", keyword).or().like("lawyer_name", keyword));
        }
        return wrapper;
    }

    private Appointment requireOwnAppointment(Long userId, Long id) {
        Appointment appointment = getById(id);
        if (appointment == null || !userId.equals(appointment.getUserId())) {
            throw new BusinessException(403, "只能访问自己的数据");
        }
        return appointment;
    }

    private Long requireUserId() {
        Long userId = SecurityUtils.currentUserId();
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
        return userId;
    }
}
