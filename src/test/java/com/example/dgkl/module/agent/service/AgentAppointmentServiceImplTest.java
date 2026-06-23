package com.example.dgkl.module.agent.service;

import com.example.dgkl.common.BusinessException;
import com.example.dgkl.module.agent.dto.AgentAppointmentRequest;
import com.example.dgkl.module.agent.service.impl.AgentAppointmentServiceImpl;
import com.example.dgkl.module.appointment.entity.Appointment;
import com.example.dgkl.module.appointment.service.AppointmentScheduleService;
import com.example.dgkl.module.appointment.service.AppointmentService;
import com.example.dgkl.module.lawfirm.entity.LawFirm;
import com.example.dgkl.module.lawfirm.service.LawFirmService;
import com.example.dgkl.module.lawyer.entity.Lawyer;
import com.example.dgkl.module.lawyer.service.LawyerService;
import com.example.dgkl.module.user.entity.SysUser;
import com.example.dgkl.security.LoginUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentAppointmentServiceImplTest {
    @Mock
    private AppointmentService appointmentService;
    @Mock
    private AppointmentScheduleService appointmentScheduleService;
    @Mock
    private LawyerService lawyerService;
    @Mock
    private LawFirmService lawFirmService;

    private AgentAppointmentServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AgentAppointmentServiceImpl(appointmentService, appointmentScheduleService, lawyerService, lawFirmService);
        SysUser user = new SysUser();
        user.setId(1001L);
        user.setUsername("user");
        LoginUser loginUser = new LoginUser(user, List.of("USER"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createsAppointmentFromConversationFields() {
        LocalDateTime time = LocalDateTime.of(2026, 6, 25, 14, 30);
        LawFirm lawFirm = lawFirm(5L, "华南律所");
        Lawyer lawyer = lawyer(9L, 5L, "李明");

        when(lawFirmService.getOne(any())).thenReturn(lawFirm);
        when(lawyerService.getOne(any())).thenReturn(lawyer);
        when(appointmentScheduleService.isSlotAvailable(9L, time)).thenReturn(true);

        Appointment result = service.create(request(time, "华南律所", "李明", "劳动纠纷"));

        ArgumentCaptor<Appointment> captor = ArgumentCaptor.forClass(Appointment.class);
        verify(appointmentService).save(captor.capture());
        Appointment saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(1001L);
        assertThat(saved.getLawFirmId()).isEqualTo(5L);
        assertThat(saved.getLawyerId()).isEqualTo(9L);
        assertThat(saved.getLawFirmName()).isEqualTo("华南律所");
        assertThat(saved.getLawyerName()).isEqualTo("李明");
        assertThat(saved.getAppointmentTime()).isEqualTo(time);
        assertThat(saved.getStatus()).isEqualTo("已预约");
        assertThat(result).isSameAs(saved);
    }

    @Test
    void cancelsOwnBookedAppointmentFromConversationFields() {
        LocalDateTime time = LocalDateTime.of(2026, 6, 25, 14, 30);
        LawFirm lawFirm = lawFirm(5L, "华南律所");
        Lawyer lawyer = lawyer(9L, 5L, "李明");
        Appointment appointment = new Appointment();
        appointment.setId(33L);
        appointment.setUserId(1001L);
        appointment.setLawFirmId(5L);
        appointment.setLawyerId(9L);
        appointment.setAppointmentTime(time);
        appointment.setStatus("已预约");

        when(lawFirmService.getOne(any())).thenReturn(lawFirm);
        when(lawyerService.getOne(any())).thenReturn(lawyer);
        when(appointmentService.getOne(any())).thenReturn(appointment);
        when(appointmentService.getById(33L)).thenReturn(appointment);

        Appointment result = service.cancel(request(time, "华南律所", "李明", null));

        verify(appointmentService).completeExpiredAppointmentsForUser(1001L);
        assertThat(appointment.getStatus()).isEqualTo("已取消");
        verify(appointmentService).updateById(appointment);
        assertThat(result).isSameAs(appointment);
    }

    @Test
    void rejectsCancelWhenNoMatchingBookedAppointmentExists() {
        LocalDateTime time = LocalDateTime.of(2026, 6, 25, 14, 30);
        when(lawFirmService.getOne(any())).thenReturn(lawFirm(5L, "华南律所"));
        when(lawyerService.getOne(any())).thenReturn(lawyer(9L, 5L, "李明"));
        when(appointmentService.getOne(any())).thenReturn(null);

        assertThatThrownBy(() -> service.cancel(request(time, "华南律所", "李明", null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("未找到可取消的预约");
    }

    private AgentAppointmentRequest request(LocalDateTime time, String lawFirmName, String lawyerName, String remark) {
        AgentAppointmentRequest request = new AgentAppointmentRequest();
        request.setAppointmentTime(time);
        request.setLawFirmName(lawFirmName);
        request.setLawyerName(lawyerName);
        request.setRemark(remark);
        return request;
    }

    private LawFirm lawFirm(Long id, String name) {
        LawFirm lawFirm = new LawFirm();
        lawFirm.setId(id);
        lawFirm.setName(name);
        lawFirm.setStatus(1);
        return lawFirm;
    }

    private Lawyer lawyer(Long id, Long lawFirmId, String name) {
        Lawyer lawyer = new Lawyer();
        lawyer.setId(id);
        lawyer.setLawFirmId(lawFirmId);
        lawyer.setName(name);
        lawyer.setStatus(1);
        return lawyer;
    }
}
