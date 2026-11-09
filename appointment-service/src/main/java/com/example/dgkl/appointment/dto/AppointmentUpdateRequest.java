package com.example.dgkl.appointment.dto;

import jakarta.validation.constraints.Size;

public record AppointmentUpdateRequest(@Size(max = 500) String remark) {
}
