package com.example.dgkl.lawyer.service;

import com.example.dgkl.common.response.ApiResponse;
import com.example.dgkl.lawyer.client.AppointmentClient;
import com.example.dgkl.lawyer.dto.ScheduleSlotResponse;
import com.example.dgkl.lawyer.entity.LawFirm;
import com.example.dgkl.lawyer.entity.Lawyer;
import com.example.dgkl.lawyer.mapper.LawFirmMapper;
import com.example.dgkl.lawyer.mapper.LawyerMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PublicDirectoryCacheTest {
    private static final String LAWYER_KEY = "smart-law:directory:v1:SMART_LAW_TEST:lawyer:11";
    private static final String LAW_FIRM_KEY = "smart-law:directory:v1:SMART_LAW_TEST:law-firm:5";

    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    private final LawFirmMapper firmMapper = mock(LawFirmMapper.class);
    private final LawyerMapper lawyerMapper = mock(LawyerMapper.class);
    private final PublicDirectoryCache cache = new PublicDirectoryCache(redis, new ObjectMapper(), firmMapper,
            lawyerMapper, "SMART_LAW_TEST", 120);

    @Test
    void cachedLawyerDetailsStillFetchFreshSchedule() {
        ValueOperations<String, String> values = values();
        when(values.get(LAWYER_KEY)).thenReturn("{\"id\":11,\"lawFirmId\":5,\"name\":\"陈律师\"}");
        when(values.get(LAW_FIRM_KEY)).thenReturn("{\"id\":5,\"name\":\"明理律师事务所\"}");
        AppointmentClient appointments = mock(AppointmentClient.class);
        ScheduleSlotResponse free = new ScheduleSlotResponse();
        free.setAvailable(true);
        ScheduleSlotResponse taken = new ScheduleSlotResponse();
        taken.setAvailable(false);
        when(appointments.schedule(11L)).thenReturn(ApiResponse.success(List.of(free)))
                .thenReturn(ApiResponse.success(List.of(taken)));
        PublicLawyerService service = new PublicLawyerService(firmMapper, lawyerMapper, appointments, cache);

        assertThat(service.lawyerDetail(11L).getScheduleSlots()).containsExactly(free);
        assertThat(service.lawyerDetail(11L).getScheduleSlots()).containsExactly(taken);
        verifyNoInteractions(firmMapper, lawyerMapper);
    }

    @Test
    void fallsBackToMysqlWhenRedisUnavailable() {
        ValueOperations<String, String> values = values();
        when(values.get(LAW_FIRM_KEY)).thenThrow(new RedisConnectionFailureException("unavailable"));
        LawFirm record = new LawFirm();
        record.setId(5L);
        when(firmMapper.selectById(5L)).thenReturn(record);

        assertThat(cache.lawFirm(5L)).isSameAs(record);
        verify(firmMapper).selectById(5L);
    }

    @Test
    void evictsOnlyAfterDatabaseCommit() {
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            cache.invalidateLawyer(11L);

            verify(redis, never()).delete(LAWYER_KEY);
            TransactionSynchronizationManager.getSynchronizations().forEach(callback -> callback.afterCommit());
            verify(redis).delete(LAWYER_KEY);
        } finally {
            TransactionSynchronizationManager.clear();
        }
    }

    @Test
    void failedEvictionBypassesPotentiallyStaleCache() {
        when(redis.delete(LAWYER_KEY)).thenThrow(new RedisConnectionFailureException("unavailable"));
        Lawyer edited = new Lawyer();
        edited.setName("更新后的律师名");
        when(lawyerMapper.selectById(11L)).thenReturn(edited);

        cache.invalidateLawyer(11L);

        assertThat(cache.lawyer(11L)).isSameAs(edited);
        verify(redis, never()).opsForValue();
    }

    @SuppressWarnings("unchecked")
    private ValueOperations<String, String> values() {
        ValueOperations<String, String> operations = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(operations);
        return operations;
    }
}
