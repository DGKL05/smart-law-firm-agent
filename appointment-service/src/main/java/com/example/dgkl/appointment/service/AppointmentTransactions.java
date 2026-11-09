package com.example.dgkl.appointment.service;

import com.example.dgkl.appointment.client.ScheduleClient;
import com.example.dgkl.appointment.dto.AppointmentBookingRequest;
import com.example.dgkl.appointment.dto.LawyerSnapshotResponse;
import com.example.dgkl.appointment.dto.ScheduleReservationRequest;
import com.example.dgkl.appointment.entity.Appointment;
import com.example.dgkl.common.exception.BusinessException;
import com.example.dgkl.common.response.ApiResponse;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.seata.core.context.RootContext;
import org.apache.seata.spring.annotation.GlobalTransactional;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentTransactions {
    private static final int TRANSACTION_TIMEOUT_MS = 20_000;

    private final AppointmentStore store;
    private final ScheduleClient scheduleClient;

    @GlobalTransactional(name = "smart-law-book-appointment", timeoutMills = TRANSACTION_TIMEOUT_MS,
            rollbackFor = Exception.class)
    public Long book(Long userId, LawyerSnapshotResponse lawyer, AppointmentBookingRequest request) {
        requireGlobalTransaction();
        Appointment appointment = store.prepare(userId, lawyer, request);
        if (AppointmentStatus.BOOKED.equals(appointment.getStatus())) {
            return appointment.getId();
        }

        ScheduleReservationRequest reservation = reservationOf(appointment);
        Boolean reserved = scheduleResult(appointment.getId(), () -> scheduleClient.reserve(reservation));
        if (reserved == null) {
            throw new BusinessException(503, "排班服务没有确认预约结果");
        }
        if (!reserved) {
            throw new BusinessException(409, "该时间段不可预约，请选择其他时间");
        }
        store.resolveBooking(appointment, true);
        return appointment.getId();
    }

    @GlobalTransactional(name = "smart-law-cancel-appointment", timeoutMills = TRANSACTION_TIMEOUT_MS,
            rollbackFor = Exception.class)
    public void cancel(Long id, Long lawyerId) {
        requireGlobalTransaction();
        Appointment appointment = store.beginCancellation(id, lawyerId);
        if (AppointmentStatus.CANCELLED.equals(appointment.getStatus())) {
            return;
        }

        scheduleResult(id, () -> scheduleClient.release(reservationOf(appointment)));
        store.resolveCancellation(appointment);
    }

    private void requireGlobalTransaction() {
        if (!RootContext.inGlobalTransaction()) {
            throw new BusinessException(503, "预约事务协调服务未就绪");
        }
    }

    private ScheduleReservationRequest reservationOf(Appointment appointment) {
        return new ScheduleReservationRequest(appointment.getId(), appointment.getLawyerId(),
                appointment.getAppointmentTime(), true);
    }

    private <T> T scheduleResult(Long appointmentId, Supplier<ApiResponse<T>> operation) {
        try {
            ApiResponse<T> response = operation.get();
            if (response != null && response.getCode() == 200) {
                return response.getData();
            }
            log.warn("排班服务未确认预约操作，预约 ID {}，XID {}", appointmentId, RootContext.getXID());
        } catch (FeignException exception) {
            log.warn("排班服务调用失败，预约 ID {}，XID {}，HTTP {}", appointmentId, RootContext.getXID(),
                    exception.status());
        }
        throw new BusinessException(503, "排班服务暂不可用，请稍后重试");
    }
}
