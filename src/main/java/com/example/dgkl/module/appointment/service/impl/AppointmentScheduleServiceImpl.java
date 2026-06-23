package com.example.dgkl.module.appointment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.dgkl.module.appointment.dto.AppointmentScheduleSlot;
import com.example.dgkl.module.appointment.entity.Appointment;
import com.example.dgkl.module.appointment.service.AppointmentScheduleService;
import com.example.dgkl.module.appointment.service.AppointmentService;
import com.example.dgkl.module.appointment.service.AppointmentStatusPolicy;
import com.example.dgkl.module.lawyer.entity.Lawyer;
import com.example.dgkl.module.lawyer.service.LawyerService;
import com.example.dgkl.module.legalcase.entity.LegalCase;
import com.example.dgkl.module.legalcase.service.LegalCaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AppointmentScheduleServiceImpl implements AppointmentScheduleService {
    private static final List<LocalTime> DEFAULT_START_TIMES = List.of(
            LocalTime.of(9, 0),
            LocalTime.of(10, 0),
            LocalTime.of(14, 0),
            LocalTime.of(15, 0));
    private static final int WORKDAY_COUNT = 5;

    private final LawyerService lawyerService;
    private final AppointmentService appointmentService;
    private final LegalCaseService legalCaseService;

    @Override
    public List<AppointmentScheduleSlot> upcomingSlots(Long lawyerId) {
        appointmentService.completeExpiredAppointments();
        Lawyer lawyer = lawyerService.getById(lawyerId);
        if (lawyer == null || lawyer.getStatus() == null || lawyer.getStatus() != 1) {
            return List.of();
        }

        List<LocalTime> startTimes = parseStartTimes(lawyer.getAvailableTimeSlots());
        List<LocalDate> dates = nextWorkdays(WORKDAY_COUNT);
        LocalDateTime from = dates.get(0).atStartOfDay();
        LocalDateTime to = dates.get(dates.size() - 1).plusDays(1).atStartOfDay();
        LocalDateTime now = LocalDateTime.now();
        Set<LocalDateTime> occupiedByAppointment = occupiedAppointmentTimes(lawyerId, from, to);
        Set<LocalDateTime> occupiedByCase = occupiedCaseTimes(lawyerId, from, to);

        List<AppointmentScheduleSlot> slots = new ArrayList<>();
        for (LocalDate date : dates) {
            for (LocalTime startTime : startTimes) {
                LocalDateTime start = LocalDateTime.of(date, startTime);
                LocalDateTime end = start.plusHours(1);
                String unavailableReason = unavailableReason(start, now, occupiedByAppointment, occupiedByCase);
                slots.add(new AppointmentScheduleSlot(date, start, end, unavailableReason == null, unavailableReason));
            }
        }
        return slots;
    }

    @Override
    public boolean isSlotAvailable(Long lawyerId, LocalDateTime appointmentTime) {
        if (lawyerId == null || appointmentTime == null) {
            return false;
        }
        return upcomingSlots(lawyerId).stream()
                .anyMatch(slot -> slot.isAvailable() && slot.getStartTime().equals(appointmentTime));
    }

    private String unavailableReason(LocalDateTime start,
                                     LocalDateTime now,
                                     Set<LocalDateTime> occupiedByAppointment,
                                     Set<LocalDateTime> occupiedByCase) {
        if (!AppointmentStatusPolicy.isBookableStart(start, now)) {
            return "已过期";
        }
        if (occupiedByAppointment.contains(start)) {
            return "已被预约";
        }
        if (occupiedByCase.contains(start)) {
            return "案件安排";
        }
        return null;
    }

    private Set<LocalDateTime> occupiedAppointmentTimes(Long lawyerId, LocalDateTime from, LocalDateTime to) {
        List<Appointment> appointments = appointmentService.list(new QueryWrapper<Appointment>()
                .eq("lawyer_id", lawyerId)
                .eq("status", AppointmentStatusPolicy.STATUS_BOOKED)
                .ge("appointment_time", from)
                .lt("appointment_time", to));
        Set<LocalDateTime> times = new HashSet<>();
        for (Appointment appointment : appointments) {
            if (appointment.getAppointmentTime() != null) {
                times.add(appointment.getAppointmentTime());
            }
        }
        return times;
    }

    private Set<LocalDateTime> occupiedCaseTimes(Long lawyerId, LocalDateTime from, LocalDateTime to) {
        List<LegalCase> legalCases = legalCaseService.list(new QueryWrapper<LegalCase>()
                .eq("lawyer_id", lawyerId)
                .notIn("status", List.of("已取消", "已结案"))
                .isNotNull("case_time")
                .ge("case_time", from)
                .lt("case_time", to));
        Set<LocalDateTime> times = new HashSet<>();
        for (LegalCase legalCase : legalCases) {
            if (legalCase.getCaseTime() != null) {
                times.add(legalCase.getCaseTime());
            }
        }
        return times;
    }

    private List<LocalDate> nextWorkdays(int count) {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate date = LocalDate.now();
        while (dates.size() < count) {
            DayOfWeek dayOfWeek = date.getDayOfWeek();
            if (dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY) {
                dates.add(date);
            }
            date = date.plusDays(1);
        }
        return dates;
    }

    private List<LocalTime> parseStartTimes(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT_START_TIMES;
        }
        List<LocalTime> times = new ArrayList<>();
        for (String item : value.split("[,，;；、]")) {
            String text = item.trim();
            if (text.isEmpty()) {
                continue;
            }
            String start = text.contains("-") ? text.substring(0, text.indexOf('-')).trim() : text;
            if (start.length() == 5) {
                times.add(LocalTime.parse(start));
            }
        }
        return times.isEmpty() ? DEFAULT_START_TIMES : times;
    }
}
