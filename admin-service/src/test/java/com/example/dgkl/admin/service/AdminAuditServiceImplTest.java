package com.example.dgkl.admin.service;

import com.example.dgkl.admin.dto.AuditRequests;
import com.example.dgkl.admin.entity.AdminAuditEvent;
import com.example.dgkl.admin.mapper.AdminAuditEventMapper;
import com.example.dgkl.admin.service.impl.AdminAuditServiceImpl;
import com.example.dgkl.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class AdminAuditServiceImplTest {
    @Test
    void automaticAuditCannotBeModifiedOrRemoved() {
        AdminAuditEventMapper mapper = mock(AdminAuditEventMapper.class);
        AdminAuditEvent automatic = new AdminAuditEvent();
        automatic.setId(10L);
        automatic.setSource("GATEWAY");
        when(mapper.selectById(10L)).thenReturn(automatic);
        AdminAuditService service = new AdminAuditServiceImpl(mapper);

        assertThatThrownBy(() -> service.update(10L, new AuditRequests.Edit("新的记录")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("系统审计记录只读");
        assertThatThrownBy(() -> service.delete(10L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("系统审计记录只读");
        verify(mapper, org.mockito.Mockito.times(2)).selectById(10L);
        verifyNoMoreInteractions(mapper);
    }
}
