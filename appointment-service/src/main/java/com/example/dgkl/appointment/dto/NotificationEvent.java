package com.example.dgkl.appointment.dto;

public record NotificationEvent(String eventKey, Long userId, String title, String content) {
    public static final String CONSULTATION_KEY_PREFIX = "consultation:";

    public static String firstConsultationReplyKey(Long consultationId) {
        return CONSULTATION_KEY_PREFIX + consultationId + ":first-reply";
    }
}
