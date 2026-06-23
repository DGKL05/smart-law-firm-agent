package com.example.dgkl.module.appointment.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.dgkl.common.BusinessException;
import com.example.dgkl.common.PageResult;
import com.example.dgkl.common.Result;
import com.example.dgkl.module.appointment.dto.AppointmentBookingRequest;
import com.example.dgkl.module.appointment.entity.Appointment;
import com.example.dgkl.module.appointment.service.AppointmentScheduleService;
import com.example.dgkl.module.appointment.service.AppointmentService;
import com.example.dgkl.module.appointment.service.AppointmentStatusPolicy;
import com.example.dgkl.module.common.AbstractUserCrudController;
import com.example.dgkl.module.common.EntityLifecycle;
import com.example.dgkl.module.lawfirm.entity.LawFirm;
import com.example.dgkl.module.lawfirm.service.LawFirmService;
import com.example.dgkl.module.lawyer.entity.Lawyer;
import com.example.dgkl.module.lawyer.service.LawyerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me/appointments")
@RequiredArgsConstructor
public class MyAppointmentController extends AbstractUserCrudController<Appointment> {
    private final AppointmentService appointmentService;
    private final AppointmentScheduleService appointmentScheduleService;
    private final LawyerService lawyerService;
    private final LawFirmService lawFirmService;

    @Override
    protected IService<Appointment> service() {
        return appointmentService;
    }

    @Override
    protected QueryWrapper<Appointment> userQuery(Long userId, String keyword) {
        QueryWrapper<Appointment> wrapper = new QueryWrapper<Appointment>().eq("user_id", userId).orderByDesc("create_time");
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(item -> item.like("law_firm_name", keyword).or().like("lawyer_name", keyword));
        }
        return wrapper;
    }

    @Override
    @GetMapping
    public Result<PageResult<Appointment>> page(@RequestParam(defaultValue = "1") long pageNum,
                                                @RequestParam(defaultValue = "10") long pageSize,
                                                @RequestParam(required = false) String keyword) {
        Long userId = requireUserId();
        appointmentService.completeExpiredAppointmentsForUser(userId);
        return Result.success(PageResult.of(appointmentService.page(new Page<>(pageNum, pageSize), userQuery(userId, keyword))));
    }

    @PostMapping("/book")
    public Result<Appointment> book(@RequestBody AppointmentBookingRequest request) {
        if (request.getLawyerId() == null || request.getAppointmentTime() == null) {
            throw new BusinessException("请选择律师和预约时间");
        }
        Lawyer lawyer = lawyerService.getById(request.getLawyerId());
        if (lawyer == null || lawyer.getStatus() == null || lawyer.getStatus() != 1) {
            throw new BusinessException("律师不存在或不可预约");
        }
        if (!appointmentScheduleService.isSlotAvailable(request.getLawyerId(), request.getAppointmentTime())) {
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
        appointment.setStatus("已预约");
        EntityLifecycle.forCreate(appointment);
        appointmentService.save(appointment);
        return Result.success(appointment);
    }

    @Override
    @PostMapping
    public Result<Appointment> create(@RequestBody Appointment appointment) {
        AppointmentBookingRequest request = new AppointmentBookingRequest();
        request.setLawyerId(appointment.getLawyerId());
        request.setAppointmentTime(appointment.getAppointmentTime());
        request.setRemark(appointment.getRemark());
        return book(request);
    }

    @PutMapping("/{id}/cancel")
    public Result<Appointment> cancel(@PathVariable Long id) {
        appointmentService.completeExpiredAppointmentsForUser(requireUserId());
        Appointment appointment = requireOwnRecord(id);
        if (!AppointmentStatusPolicy.STATUS_BOOKED.equals(appointment.getStatus())) {
            throw new BusinessException("只有已预约且未结束的预约可以取消");
        }
        appointment.setStatus("已取消");
        EntityLifecycle.forUpdate(appointment);
        appointmentService.updateById(appointment);
        return Result.success(appointmentService.getById(id));
    }
}
