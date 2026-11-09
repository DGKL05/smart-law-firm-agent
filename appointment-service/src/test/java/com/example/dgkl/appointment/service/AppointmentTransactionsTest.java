package com.example.dgkl.appointment.service;

import com.example.dgkl.appointment.client.ScheduleClient;
import com.example.dgkl.appointment.dto.AppointmentBookingRequest;
import com.example.dgkl.appointment.dto.LawyerSnapshotResponse;
import com.example.dgkl.appointment.dto.ScheduleReservationRequest;
import com.example.dgkl.appointment.entity.Appointment;
import com.example.dgkl.common.exception.BusinessException;
import com.example.dgkl.common.response.ApiResponse;
import org.apache.seata.core.context.RootContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AppointmentTransactionsTest {
    private final AppointmentStore store = mock(AppointmentStore.class);
    private final ScheduleClient schedule = mock(ScheduleClient.class);
    private final AppointmentTransactions transactions = new AppointmentTransactions(store, schedule);

    @AfterEach
    void clearTransactionContext() {
        RootContext.unbind();
    }

    @Test
    void bookingWithoutAnXidDoesNotWriteAnyBranch() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> transactions.book(1L, new LawyerSnapshotResponse(), new AppointmentBookingRequest()));

        assertEquals(503, exception.getCode());
        verifyNoInteractions(store, schedule);
    }

    @Test
    void bookingConflictEscapesTheGlobalMethodBeforeConfirmation() {
        RootContext.bind("127.0.0.1:8091:1");
        LawyerSnapshotResponse lawyer = new LawyerSnapshotResponse();
        AppointmentBookingRequest request = new AppointmentBookingRequest();
        Appointment appointment = appointmentInState(AppointmentStatus.BOOKING);
        when(store.prepare(1L, lawyer, request)).thenReturn(appointment);
        when(schedule.reserve(reservationOf(appointment))).thenReturn(ApiResponse.success(false));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> transactions.book(1L, lawyer, request));

        assertEquals(409, exception.getCode());
        verify(store, never()).resolveBooking(appointment, false);
        verify(store, never()).resolveBooking(appointment, true);
    }

    @Test
    void failedReleaseEscapesTheGlobalMethodBeforeCancellationIsConfirmed() {
        RootContext.bind("127.0.0.1:8091:2");
        Appointment appointment = appointmentInState(AppointmentStatus.CANCELLING);
        when(store.beginCancellation(appointment.getId(), appointment.getLawyerId())).thenReturn(appointment);
        when(schedule.release(reservationOf(appointment))).thenReturn(ApiResponse.fail(503, "retry"));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> transactions.cancel(appointment.getId(), appointment.getLawyerId()));

        assertEquals(503, exception.getCode());
        verify(store, never()).resolveCancellation(appointment);
    }

    private static Appointment appointmentInState(String status) {
        Appointment appointment = new Appointment();
        appointment.setId(1L);
        appointment.setLawyerId(2L);
        appointment.setAppointmentTime(LocalDateTime.of(2026, 12, 1, 10, 0));
        appointment.setStatus(status);
        return appointment;
    }

    private static ScheduleReservationRequest reservationOf(Appointment appointment) {
        return new ScheduleReservationRequest(appointment.getId(), appointment.getLawyerId(),
                appointment.getAppointmentTime(), true);
    }
}
