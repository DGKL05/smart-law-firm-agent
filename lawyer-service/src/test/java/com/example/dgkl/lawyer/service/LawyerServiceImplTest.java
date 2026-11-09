package com.example.dgkl.lawyer.service;

import com.example.dgkl.common.exception.BusinessException;
import com.example.dgkl.common.response.ApiResponse;
import com.example.dgkl.lawyer.client.AppointmentClient;
import com.example.dgkl.lawyer.client.AuthUserClient;
import com.example.dgkl.lawyer.entity.Lawyer;
import com.example.dgkl.lawyer.mapper.LawFirmMapper;
import com.example.dgkl.lawyer.mapper.LawyerMapper;
import com.example.dgkl.lawyer.service.impl.LawyerServiceImpl;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class LawyerServiceImplTest {
    @Test
    void refusesToDeleteDisabledLawyerWithActiveWork() {
        LawyerMapper mapper = mock(LawyerMapper.class);
        AppointmentClient appointments = mock(AppointmentClient.class);
        LawyerMediaService media = mock(LawyerMediaService.class);
        Lawyer lawyer = new Lawyer();
        lawyer.setId(17L);
        lawyer.setStatus(0);
        when(mapper.selectOne(any())).thenReturn(lawyer);
        when(appointments.hasActiveWork(17L)).thenReturn(ApiResponse.success(true));
        LawyerService service = new LawyerServiceImpl(mapper, mock(LawFirmMapper.class),
                mock(AuthUserClient.class), appointments, media, mock(PublicDirectoryCache.class));

        assertThatThrownBy(() -> service.delete(17L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("请先处理该律师的预约、案件和咨询");
        verify(mapper, never()).deleteById(17L);
        verifyNoInteractions(media);
    }
}
