package com.example.dgkl.module.appointment.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class AppointmentStatusPolicyTest {
    @Test
    void appointmentCompletesAfterConsultationWindow() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 23, 11, 0);

        assertThat(AppointmentStatusPolicy.isExpired(LocalDateTime.of(2026, 6, 23, 9, 59), now)).isTrue();
        assertThat(AppointmentStatusPolicy.isExpired(LocalDateTime.of(2026, 6, 23, 10, 1), now)).isFalse();
    }

    @Test
    void cannotBookSlotAfterItHasStarted() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 23, 9, 1);

        assertThat(AppointmentStatusPolicy.isBookableStart(LocalDateTime.of(2026, 6, 23, 9, 0), now)).isFalse();
        assertThat(AppointmentStatusPolicy.isBookableStart(LocalDateTime.of(2026, 6, 23, 10, 0), now)).isTrue();
    }
}
