package com.example.dgkl.appointment.service;

import com.example.dgkl.appointment.client.ScheduleClient;
import com.example.dgkl.appointment.dto.ScheduleReservationRequest;
import com.example.dgkl.appointment.entity.Appointment;
import com.example.dgkl.common.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.time.LocalDateTime;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppointmentSyncTest {
    private final ScheduleClient client = mock(ScheduleClient.class);
    private final AppointmentStore store = mock(AppointmentStore.class);
    private final AppointmentSync sync = new AppointmentSync(client, store);

    @Test
    void keepsBookingPendingWhenResponseIsLostAndRetriesTheSameReservation() {
        Appointment appointment = pending(AppointmentStatus.BOOKING);
        ScheduleReservationRequest request = request(appointment);
        when(client.reserve(request)).thenThrow(new IllegalStateException("lost response"))
                .thenReturn(ApiResponse.success(true));

        sync.sync(appointment);
        verify(store).defer(appointment);
        verify(store, never()).resolveBooking(appointment, true);

        sync.sync(appointment);
        InOrder order = inOrder(client, store);
        order.verify(client).reserve(request);
        order.verify(store).defer(appointment);
        order.verify(client).reserve(request);
        order.verify(store).resolveBooking(appointment, true);
    }

    @Test
    void marksCancellationOnlyWhenTheScheduleConfirmsRelease() {
        Appointment appointment = pending(AppointmentStatus.CANCELLING);
        ScheduleReservationRequest request = request(appointment);
        when(client.release(request)).thenReturn(ApiResponse.fail(503, "retry")).thenReturn(ApiResponse.success());

        sync.sync(appointment);
        verify(store).defer(appointment);
        verify(store, never()).resolveCancellation(appointment);

        sync.sync(appointment);
        verify(store).resolveCancellation(appointment);
    }

    private static Appointment pending(String status) {
        Appointment appointment = new Appointment();
        appointment.setId(12L);
        appointment.setLawyerId(24L);
        appointment.setAppointmentTime(LocalDateTime.of(2026, 11, 10, 10, 0));
        appointment.setStatus(status);
        return appointment;
    }

    private static ScheduleReservationRequest request(Appointment appointment) {
        return new ScheduleReservationRequest(appointment.getId(), appointment.getLawyerId(),
                appointment.getAppointmentTime());
    }
}
