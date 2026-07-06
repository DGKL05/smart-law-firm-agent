package com.example.dgkl.appointment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.dgkl.appointment.client.LawyerClient;
import com.example.dgkl.appointment.dto.AppointmentBookingRequest;
import com.example.dgkl.appointment.dto.LawyerSnapshotResponse;
import com.example.dgkl.appointment.dto.ScheduleSlotResponse;
import com.example.dgkl.appointment.entity.Appointment;
import com.example.dgkl.appointment.entity.LawyerSchedule;
import com.example.dgkl.appointment.entity.LegalCase;
import com.example.dgkl.appointment.mapper.AppointmentMapper;
import com.example.dgkl.appointment.mapper.LawyerScheduleMapper;
import com.example.dgkl.appointment.mapper.LegalCaseMapper;
import com.example.dgkl.common.exception.BusinessException;
import com.example.dgkl.common.response.PageResult;
import com.example.dgkl.common.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AppointmentService {
    public static final String STATUS_BOOKED = "已预约";
    public static final String STATUS_CANCELLED = "已取消";

    private final AppointmentMapper appointmentMapper;
    private final LawyerScheduleMapper lawyerScheduleMapper;
    private final LegalCaseMapper legalCaseMapper;
    private final LawyerClient lawyerClient;

    public PageResult<Appointment> userPage(long pageNum, long pageSize) {
        Long userId = requireUserId();
        Page<Appointment> page = appointmentMapper.selectPage(Page.of(pageNum, pageSize), new LambdaQueryWrapper<Appointment>()
                .eq(Appointment::getUserId, userId)
                .eq(Appointment::getDeleted, 0)
                .orderByDesc(Appointment::getCreateTime));
        return new PageResult<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    public PageResult<Appointment> lawyerPage(long pageNum, long pageSize) {
        Long userId = requireUserId();
        LawyerSnapshotResponse lawyer = lawyerClient.snapshotByUserId(userId).getData();
        if (lawyer == null) {
            throw new BusinessException("当前账号未绑定律师资料");
        }
        Long lawyerId = lawyer.getId();
        Page<Appointment> page = appointmentMapper.selectPage(Page.of(pageNum, pageSize), new LambdaQueryWrapper<Appointment>()
                .eq(Appointment::getLawyerId, lawyerId)
                .eq(Appointment::getDeleted, 0)
                .orderByDesc(Appointment::getAppointmentTime));
        return new PageResult<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    public Appointment book(AppointmentBookingRequest request) {
        if (request == null || request.getLawyerId() == null || request.getAppointmentTime() == null) {
            throw new BusinessException("请选择律师和预约时间");
        }
        if (!isSlotAvailable(request.getLawyerId(), request.getAppointmentTime())) {
            throw new BusinessException("该时间段不可预约，请选择其他时间");
        }
        LawyerSnapshotResponse lawyer = lawyerClient.snapshot(request.getLawyerId()).getData();
        if (lawyer == null || lawyer.getStatus() == null || lawyer.getStatus() != 1) {
            throw new BusinessException("律师不存在或不可预约");
        }
        Appointment appointment = new Appointment();
        appointment.setUserId(requireUserId());
        appointment.setLawyerId(lawyer.getId());
        appointment.setLawFirmId(lawyer.getLawFirmId());
        appointment.setLawyerName(lawyer.getName());
        appointment.setLawFirmName(lawyer.getLawFirmName());
        appointment.setAppointmentTime(request.getAppointmentTime());
        appointment.setDurationMinutes(60);
        appointment.setStatus(STATUS_BOOKED);
        appointment.setRemark(request.getRemark());
        appointment.setDeleted(0);
        appointmentMapper.insert(appointment);
        return appointment;
    }

    public Appointment cancel(Long id) {
        Long userId = requireUserId();
        Appointment appointment = appointmentMapper.selectById(id);
        if (appointment == null || !userId.equals(appointment.getUserId())) {
            throw new BusinessException(403, "只能取消自己的预约");
        }
        if (!STATUS_BOOKED.equals(appointment.getStatus())) {
            throw new BusinessException("只有已预约状态可以取消");
        }
        appointment.setStatus(STATUS_CANCELLED);
        appointmentMapper.updateById(appointment);
        return appointmentMapper.selectById(id);
    }

    public List<ScheduleSlotResponse> schedule(Long lawyerId) {
        List<ScheduleSlotResponse> slots = new ArrayList<>();
        LocalDate cursor = LocalDate.now();
        while (slots.size() < 25) {
            cursor = cursor.plusDays(1);
            if (cursor.getDayOfWeek() == DayOfWeek.SATURDAY || cursor.getDayOfWeek() == DayOfWeek.SUNDAY) {
                continue;
            }
            for (LocalTime start : dayStarts(lawyerId, cursor)) {
                LocalDateTime startTime = LocalDateTime.of(cursor, start);
                LocalDateTime endTime = startTime.plusHours(1);
                String reason = unavailableReason(lawyerId, startTime, endTime);
                slots.add(new ScheduleSlotResponse(cursor, startTime, endTime, reason == null, reason));
            }
        }
        return slots;
    }

    public boolean isSlotAvailable(Long lawyerId, LocalDateTime startTime) {
        return unavailableReason(lawyerId, startTime, startTime.plusHours(1)) == null;
    }

    private List<LocalTime> dayStarts(Long lawyerId, LocalDate date) {
        List<LawyerSchedule> schedules = lawyerScheduleMapper.selectList(new LambdaQueryWrapper<LawyerSchedule>()
                .eq(LawyerSchedule::getLawyerId, lawyerId)
                .eq(LawyerSchedule::getWeekday, date.getDayOfWeek().getValue())
                .eq(LawyerSchedule::getEnabled, 1)
                .eq(LawyerSchedule::getDeleted, 0));
        if (schedules.isEmpty()) {
            return List.of(LocalTime.of(9, 0), LocalTime.of(10, 0), LocalTime.of(11, 0),
                    LocalTime.of(14, 0), LocalTime.of(15, 0), LocalTime.of(16, 0));
        }
        return schedules.stream()
                .flatMap(schedule -> hourly(schedule.getStartTime(), schedule.getEndTime()).stream())
                .toList();
    }

    private List<LocalTime> hourly(LocalTime start, LocalTime end) {
        List<LocalTime> times = new ArrayList<>();
        LocalTime cursor = start;
        while (cursor.plusHours(1).compareTo(end) <= 0) {
            times.add(cursor);
            cursor = cursor.plusHours(1);
        }
        return times;
    }

    private String unavailableReason(Long lawyerId, LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime.isBefore(LocalDateTime.now())) {
            return "已过期";
        }
        Long appointmentCount = appointmentMapper.selectCount(new LambdaQueryWrapper<Appointment>()
                .eq(Appointment::getLawyerId, lawyerId)
                .eq(Appointment::getAppointmentTime, startTime)
                .eq(Appointment::getStatus, STATUS_BOOKED)
                .eq(Appointment::getDeleted, 0));
        if (appointmentCount > 0) {
            return "已预约";
        }
        Long caseCount = legalCaseMapper.selectCount(new LambdaQueryWrapper<LegalCase>()
                .eq(LegalCase::getLawyerId, lawyerId)
                .eq(LegalCase::getDeleted, 0)
                .lt(LegalCase::getStartTime, endTime)
                .gt(LegalCase::getEndTime, startTime));
        if (caseCount > 0) {
            return "案件占用";
        }
        return null;
    }

    private Long requireUserId() {
        Long userId = SecurityUtils.currentUserId();
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
        return userId;
    }
}
