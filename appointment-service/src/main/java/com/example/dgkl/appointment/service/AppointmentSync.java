package com.example.dgkl.appointment.service;

import com.example.dgkl.appointment.client.ScheduleClient;
import com.example.dgkl.appointment.dto.ScheduleReservationRequest;
import com.example.dgkl.appointment.entity.Appointment;
import com.example.dgkl.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class AppointmentSync {
    private final ScheduleClient scheduleClient;
    private final AppointmentStore store;

    public void sync(Appointment appointment) {
        ScheduleReservationRequest request = new ScheduleReservationRequest(appointment.getId(),
                appointment.getLawyerId(), appointment.getAppointmentTime());
        if (AppointmentStatus.BOOKING.equals(appointment.getStatus())) {
            ApiResponse<Boolean> reservation = call(appointment, () -> scheduleClient.reserve(request));
            if (reservation == null || reservation.getCode() != 200 || reservation.getData() == null) {
                store.defer(appointment);
                return;
            }
            store.resolveBooking(appointment, reservation.getData());
        } else if (AppointmentStatus.CANCELLING.equals(appointment.getStatus())) {
            ApiResponse<Void> release = call(appointment, () -> scheduleClient.release(request));
            if (release == null || release.getCode() != 200) {
                store.defer(appointment);
                return;
            }
            store.resolveCancellation(appointment);
        }
    }

    @Scheduled(fixedDelayString = "${appointment.sync-delay-ms:5000}")
    public void reconcile() {
        for (Appointment appointment : store.due()) {
            try {
                sync(appointment);
            } catch (RuntimeException exception) {
                log.error("Failed to reconcile appointment {} in state {}", appointment.getId(),
                        appointment.getStatus(), exception);
                store.defer(appointment);
            }
        }
    }

    private <T> ApiResponse<T> call(Appointment appointment, Supplier<ApiResponse<T>> operation) {
        try {
            ApiResponse<T> response = operation.get();
            if (response == null || response.getCode() != 200) {
                log.warn("Schedule service did not confirm appointment {} in state {}", appointment.getId(),
                        appointment.getStatus());
            }
            return response;
        } catch (RuntimeException exception) {
            log.warn("Schedule call failed for appointment {} in state {}: {}", appointment.getId(),
                    appointment.getStatus(), exception.getClass().getSimpleName());
            return null;
        }
    }
}
