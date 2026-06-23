package com.example.dgkl.module.agent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.dgkl.common.BusinessException;
import com.example.dgkl.module.agent.dto.AgentAppointmentRequest;
import com.example.dgkl.module.agent.service.AgentAppointmentService;
import com.example.dgkl.module.appointment.entity.Appointment;
import com.example.dgkl.module.appointment.service.AppointmentScheduleService;
import com.example.dgkl.module.appointment.service.AppointmentService;
import com.example.dgkl.module.appointment.service.AppointmentStatusPolicy;
import com.example.dgkl.module.common.EntityLifecycle;
import com.example.dgkl.module.lawfirm.entity.LawFirm;
import com.example.dgkl.module.lawfirm.service.LawFirmService;
import com.example.dgkl.module.lawyer.entity.Lawyer;
import com.example.dgkl.module.lawyer.service.LawyerService;
import com.example.dgkl.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AgentAppointmentServiceImpl implements AgentAppointmentService {
    private final AppointmentService appointmentService;
    private final AppointmentScheduleService appointmentScheduleService;
    private final LawyerService lawyerService;
    private final LawFirmService lawFirmService;

    @Override
    public Appointment create(AgentAppointmentRequest request) {
        validateRequiredFields(request);
        Long userId = requireUserId();
        LawFirm lawFirm = findLawFirm(request.getLawFirmName());
        Lawyer lawyer = findLawyer(lawFirm.getId(), request.getLawyerName());

        if (!appointmentScheduleService.isSlotAvailable(lawyer.getId(), request.getAppointmentTime())) {
            throw new BusinessException("该时间段不可预约，请选择其他时间");
        }

        Appointment appointment = new Appointment();
        appointment.setUserId(userId);
        appointment.setLawFirmId(lawFirm.getId());
        appointment.setLawyerId(lawyer.getId());
        appointment.setLawFirmName(lawFirm.getName());
        appointment.setLawyerName(lawyer.getName());
        appointment.setAppointmentTime(request.getAppointmentTime());
        appointment.setRemark(request.getRemark());
        appointment.setStatus(AppointmentStatusPolicy.STATUS_BOOKED);
        EntityLifecycle.forCreate(appointment);
        appointmentService.save(appointment);
        return appointment;
    }

    @Override
    public Appointment cancel(AgentAppointmentRequest request) {
        validateRequiredFields(request);
        Long userId = requireUserId();
        LawFirm lawFirm = findLawFirm(request.getLawFirmName());
        Lawyer lawyer = findLawyer(lawFirm.getId(), request.getLawyerName());

        appointmentService.completeExpiredAppointmentsForUser(userId);
        Appointment appointment = appointmentService.getOne(new QueryWrapper<Appointment>()
                .eq("user_id", userId)
                .eq("law_firm_id", lawFirm.getId())
                .eq("lawyer_id", lawyer.getId())
                .eq("appointment_time", request.getAppointmentTime())
                .eq("status", AppointmentStatusPolicy.STATUS_BOOKED)
                .eq("deleted", 0)
                .last("LIMIT 1"));
        if (appointment == null) {
            throw new BusinessException("未找到可取消的预约，请确认时间、律所和律师是否正确");
        }

        appointment.setStatus(AppointmentStatusPolicy.STATUS_CANCELLED);
        EntityLifecycle.forUpdate(appointment);
        appointmentService.updateById(appointment);
        return appointmentService.getById(appointment.getId());
    }

    private void validateRequiredFields(AgentAppointmentRequest request) {
        if (request == null || request.getAppointmentTime() == null
                || isBlank(request.getLawFirmName()) || isBlank(request.getLawyerName())) {
            throw new BusinessException("请提供准确到分钟的预约时间、律所名称和律师姓名");
        }
    }

    private LawFirm findLawFirm(String lawFirmName) {
        LawFirm lawFirm = lawFirmService.getOne(new QueryWrapper<LawFirm>()
                .eq("name", lawFirmName.trim())
                .eq("status", 1)
                .eq("deleted", 0)
                .last("LIMIT 1"));
        if (lawFirm == null) {
            throw new BusinessException("未找到该律所，请确认律所名称");
        }
        return lawFirm;
    }

    private Lawyer findLawyer(Long lawFirmId, String lawyerName) {
        Lawyer lawyer = lawyerService.getOne(new QueryWrapper<Lawyer>()
                .eq("law_firm_id", lawFirmId)
                .eq("name", lawyerName.trim())
                .eq("status", 1)
                .eq("deleted", 0)
                .last("LIMIT 1"));
        if (lawyer == null) {
            throw new BusinessException("未找到该律所下的律师，请确认律师姓名");
        }
        return lawyer;
    }

    private Long requireUserId() {
        Long userId = SecurityUtils.currentUserId();
        if (userId == null) {
            throw new BusinessException(401, "请先登录后再通过 Agent 操作预约");
        }
        return userId;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
