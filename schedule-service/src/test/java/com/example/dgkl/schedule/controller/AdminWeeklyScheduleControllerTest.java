package com.example.dgkl.schedule.controller;

import com.example.dgkl.common.exception.GlobalExceptionHandler;
import com.example.dgkl.schedule.dto.WeeklyScheduleRequest;
import com.example.dgkl.schedule.dto.WeeklyScheduleResponse;
import com.example.dgkl.schedule.dto.WeeklyScheduleRule;
import com.example.dgkl.schedule.service.ScheduleLawyers;
import com.example.dgkl.schedule.service.ScheduleService;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalTime;
import java.util.List;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminWeeklyScheduleControllerTest {
    private static final String PATH = "/api/admin/schedules/weekly";
    private static final Long LAWYER_ID = 1L;
    private static final String DEFAULT_REVISION = "a".repeat(64);
    private static final String UPDATED_REVISION = "b".repeat(64);

    private final ScheduleService service = mock(ScheduleService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new AdminWeeklyScheduleController(service),
                    new ScheduleRuleController(service, mock(ScheduleLawyers.class)))
            .setControllerAdvice(new GlobalExceptionHandler())
            .setMessageConverters(new MappingJackson2HttpMessageConverter(JsonMapper.builder()
                    .addModule(new JavaTimeModule())
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                    .build()))
            .build();

    @Test
    void adminWeeklyRouteSerializesDefaultsAndAcceptsShortLocalTimes() throws Exception {
        WeeklyScheduleRule defaultRule = new WeeklyScheduleRule(null, 1, LocalTime.of(9, 0), LocalTime.NOON, 1);
        when(service.weeklyRules(LAWYER_ID))
                .thenReturn(new WeeklyScheduleResponse(DEFAULT_REVISION, false, List.of(defaultRule)));
        WeeklyScheduleRule savedRule = new WeeklyScheduleRule(12L, 1, LocalTime.of(9, 0), LocalTime.NOON, 1);
        when(service.replaceWeeklyRules(LAWYER_ID, new WeeklyScheduleRequest(DEFAULT_REVISION, List.of(defaultRule))))
                .thenReturn(new WeeklyScheduleResponse(UPDATED_REVISION, true, List.of(savedRule)));

        mvc.perform(get(PATH).param("lawyerId", LAWYER_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rules[0].id").value(nullValue()))
                .andExpect(jsonPath("$.data.rules[0].startTime").value("09:00:00"))
                .andExpect(jsonPath("$.data.configured").value(false));
        mvc.perform(put(PATH).param("lawyerId", LAWYER_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "revision":"%s",
                                  "rules":[{"weekday":1,"startTime":"09:00","endTime":"12:00:00","enabled":1}]
                                }
                                """.formatted(DEFAULT_REVISION)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.revision").value(UPDATED_REVISION))
                .andExpect(jsonPath("$.data.rules[0].id").value(12))
                .andExpect(jsonPath("$.data.configured").value(true));
    }

    @Test
    void invalidLawyerAndRuleInputsAreRejectedBeforeTheServiceIsCalled() throws Exception {
        mvc.perform(get(PATH).param("lawyerId", "0"))
                .andExpect(status().isBadRequest());
        mvc.perform(put(PATH).param("lawyerId", LAWYER_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "revision":"%s",
                                  "rules":[{"weekday":8,"startTime":"09:00","endTime":"12:00","enabled":1}]
                                }
                                """.formatted(DEFAULT_REVISION)))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
