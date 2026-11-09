package com.example.dgkl.schedule.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LawyerSnapshot(Long id, Integer status) {
}
