package com.example.dgkl.appointment.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.dgkl.appointment.dto.LegalCaseRequest;
import com.example.dgkl.appointment.entity.Appointment;
import com.example.dgkl.appointment.entity.LegalCase;
import com.example.dgkl.appointment.mapper.AppointmentGuardMapper;
import com.example.dgkl.appointment.mapper.AppointmentMapper;
import com.example.dgkl.appointment.mapper.LegalCaseMapper;
import com.example.dgkl.appointment.service.AppointmentPeers;
import com.example.dgkl.appointment.service.AppointmentStatus;
import com.example.dgkl.appointment.service.AppointmentTimes;
import com.example.dgkl.appointment.service.CaseAvailability;
import com.example.dgkl.appointment.service.LegalCaseService;
import com.example.dgkl.common.exception.BusinessException;
import com.example.dgkl.common.response.PageResult;
import lombok.RequiredArgsConstructor;
import org.apache.seata.spring.annotation.GlobalLock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class LegalCaseServiceImpl implements LegalCaseService {
    private final LegalCaseMapper caseMapper;
    private final AppointmentMapper appointmentMapper;
    private final AppointmentGuardMapper guardMapper;
    private final AppointmentPeers peers;
    private final CaseAvailability availability;

    @Override
    public PageResult<LegalCase> page(boolean mine, Long userId, Long lawyerId, long pageNum, long pageSize) {
        Long effectiveUser = mine ? peers.currentUserId() : userId;
        Page<LegalCase> page = caseMapper.selectPage(Page.of(pageNum, pageSize), Wrappers.<LegalCase>lambdaQuery()
                .eq(effectiveUser != null, LegalCase::getUserId, effectiveUser)
                .eq(lawyerId != null, LegalCase::getLawyerId, lawyerId)
                .orderByDesc(LegalCase::getStartTime, LegalCase::getId));
        return new PageResult<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public LegalCase detail(boolean mine, Long id) {
        LegalCase legalCase = caseMapper.selectById(id);
        if (legalCase == null || (mine && !peers.currentUserId().equals(legalCase.getUserId()))) {
            throw new BusinessException(404, "案件不存在");
        }
        return legalCase;
    }

    @Override
    @GlobalLock
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public LegalCase create(LegalCaseRequest request) {
        validateReferences(request);
        LegalCase legalCase = new LegalCase();
        apply(legalCase, request);
        lockLawyers(null, legalCase.getLawyerId());
        validateConflicts(legalCase);
        caseMapper.insert(legalCase);
        return detail(false, legalCase.getId());
    }

    @Override
    @GlobalLock
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public LegalCase update(Long id, LegalCaseRequest request) {
        validateReferences(request);
        LegalCase legalCase = lockCase(id);
        Long previousLawyer = legalCase.getLawyerId();
        apply(legalCase, request);
        lockLawyers(previousLawyer, legalCase.getLawyerId());
        validateConflicts(legalCase);
        caseMapper.update(null, Wrappers.<LegalCase>lambdaUpdate()
                .eq(LegalCase::getId, id)
                .set(LegalCase::getUserId, legalCase.getUserId())
                .set(LegalCase::getLawyerId, legalCase.getLawyerId())
                .set(LegalCase::getCaseNo, legalCase.getCaseNo())
                .set(LegalCase::getTitle, legalCase.getTitle())
                .set(LegalCase::getCaseType, legalCase.getCaseType())
                .set(LegalCase::getDescription, legalCase.getDescription())
                .set(LegalCase::getStartTime, legalCase.getStartTime())
                .set(LegalCase::getEndTime, legalCase.getEndTime())
                .set(LegalCase::getStatus, legalCase.getStatus()));
        return detail(false, id);
    }

    @Override
    @GlobalLock
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public void delete(Long id) {
        LegalCase legalCase = lockCase(id);
        lockLawyers(legalCase.getLawyerId(), null);
        caseMapper.deleteById(id);
    }

    @Override
    public long count() {
        return caseMapper.selectCount(Wrappers.<LegalCase>lambdaQuery());
    }

    private LegalCase lockCase(Long id) {
        LegalCase legalCase = caseMapper.selectOne(Wrappers.<LegalCase>lambdaQuery()
                .eq(LegalCase::getId, id)
                .last("FOR UPDATE"));
        if (legalCase == null) {
            throw new BusinessException(404, "案件不存在");
        }
        return legalCase;
    }

    private void validateReferences(LegalCaseRequest request) {
        peers.requireUser(request.userId());
        if (request.lawyerId() != null) {
            peers.lawyer(request.lawyerId());
        }
    }

    private void lockLawyers(Long previousLawyer, Long nextLawyer) {
        Stream.of(previousLawyer, nextLawyer).filter(Objects::nonNull).distinct().sorted().forEach(guardMapper::lock);
    }

    private void apply(LegalCase legalCase, LegalCaseRequest request) {
        LocalDateTime start = request.startTime();
        LocalDateTime end = request.endTime() == null ? AppointmentTimes.slotEnd(start) : request.endTime();
        if (!start.isBefore(end) || start.getNano() != 0 || end.getNano() != 0) {
            throw new BusinessException("案件结束时间须晚于开始时间，且精确到秒");
        }
        legalCase.setUserId(request.userId());
        legalCase.setLawyerId(request.lawyerId());
        legalCase.setCaseNo(request.caseNo().trim());
        legalCase.setTitle(request.title().trim());
        legalCase.setCaseType(request.caseType());
        legalCase.setDescription(request.description());
        legalCase.setStartTime(start);
        legalCase.setEndTime(end);
        if (request.status() != null) {
            legalCase.setStatus(request.status());
        } else if (legalCase.getStatus() == null) {
            legalCase.setStatus(CaseAvailability.ACTIVE);
        }
    }

    private void validateConflicts(LegalCase legalCase) {
        Long lawyerId = legalCase.getLawyerId();
        if (lawyerId == null || !CaseAvailability.ACTIVE.equals(legalCase.getStatus())) {
            return;
        }
        LocalDateTime start = legalCase.getStartTime();
        LocalDateTime end = legalCase.getEndTime();
        if (availability.overlapping(lawyerId, start, end).stream()
                .anyMatch(other -> !Objects.equals(legalCase.getId(), other.getId()))) {
            throw new BusinessException(409, "案件时间与律师的其他案件冲突");
        }
        Long conflicts = appointmentMapper.selectCount(Wrappers.<Appointment>lambdaQuery()
                .eq(Appointment::getLawyerId, lawyerId)
                .in(Appointment::getStatus, AppointmentStatus.ACTIVE)
                .lt(Appointment::getAppointmentTime, end)
                .gt(Appointment::getAppointmentTime, start.minusMinutes(AppointmentTimes.SLOT_DURATION_MINUTES)));
        if (conflicts > 0) {
            throw new BusinessException(409, "案件时间与有效预约冲突");
        }
    }
}
