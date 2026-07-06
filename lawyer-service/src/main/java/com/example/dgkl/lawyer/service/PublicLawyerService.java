package com.example.dgkl.lawyer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.dgkl.common.exception.BusinessException;
import com.example.dgkl.common.response.PageResult;
import com.example.dgkl.lawyer.client.AppointmentClient;
import com.example.dgkl.lawyer.dto.LawFirmDetailResponse;
import com.example.dgkl.lawyer.dto.LawyerResponse;
import com.example.dgkl.lawyer.dto.ScheduleSlotResponse;
import com.example.dgkl.lawyer.entity.LawFirm;
import com.example.dgkl.lawyer.entity.Lawyer;
import com.example.dgkl.lawyer.mapper.LawFirmMapper;
import com.example.dgkl.lawyer.mapper.LawyerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PublicLawyerService {
    private final LawFirmMapper lawFirmMapper;
    private final LawyerMapper lawyerMapper;
    private final AppointmentClient appointmentClient;

    public PageResult<LawFirm> lawFirms(int pageNum, int pageSize, String provinceCodes, String keyword) {
        LambdaQueryWrapper<LawFirm> query = new LambdaQueryWrapper<LawFirm>()
                .eq(LawFirm::getDeleted, 0)
                .orderByDesc(LawFirm::getId);
        if (StringUtils.hasText(provinceCodes)) {
            query.in(LawFirm::getProvinceCode, Arrays.stream(provinceCodes.split(",")).filter(StringUtils::hasText).toList());
        }
        if (StringUtils.hasText(keyword)) {
            query.and(wrapper -> wrapper.like(LawFirm::getName, keyword)
                    .or().like(LawFirm::getCity, keyword)
                    .or().like(LawFirm::getSpecialties, keyword));
        }
        Page<LawFirm> page = lawFirmMapper.selectPage(Page.of(pageNum, pageSize), query);
        return new PageResult<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    public LawFirmDetailResponse lawFirmDetail(Long id) {
        LawFirm firm = lawFirmMapper.selectById(id);
        if (firm == null || Integer.valueOf(1).equals(firm.getDeleted())) {
            throw new BusinessException("律所不存在");
        }
        List<LawyerResponse> lawyers = lawyerMapper.selectList(new LambdaQueryWrapper<Lawyer>()
                        .eq(Lawyer::getLawFirmId, id)
                        .eq(Lawyer::getDeleted, 0))
                .stream()
                .map(lawyer -> LawyerResponse.of(lawyer, firm.getName(), safeSchedule(lawyer.getId())))
                .toList();
        return LawFirmDetailResponse.of(firm, lawyers);
    }

    public PageResult<LawyerResponse> lawyers(int pageNum, int pageSize, String category, Long lawFirmId, String keyword) {
        LambdaQueryWrapper<Lawyer> query = new LambdaQueryWrapper<Lawyer>()
                .eq(Lawyer::getDeleted, 0)
                .orderByDesc(Lawyer::getId);
        if (StringUtils.hasText(category)) {
            query.eq(Lawyer::getCategory, category);
        }
        if (lawFirmId != null) {
            query.eq(Lawyer::getLawFirmId, lawFirmId);
        }
        if (StringUtils.hasText(keyword)) {
            query.and(wrapper -> wrapper.like(Lawyer::getName, keyword).or().like(Lawyer::getGoodAt, keyword));
        }
        Page<Lawyer> page = lawyerMapper.selectPage(Page.of(pageNum, pageSize), query);
        List<LawyerResponse> records = page.getRecords().stream()
                .map(this::toResponseWithoutSchedule)
                .toList();
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    public LawyerResponse lawyerDetail(Long id) {
        Lawyer lawyer = lawyerMapper.selectById(id);
        if (lawyer == null || Integer.valueOf(1).equals(lawyer.getDeleted())) {
            throw new BusinessException("律师不存在");
        }
        LawFirm firm = lawFirmMapper.selectById(lawyer.getLawFirmId());
        return LawyerResponse.of(lawyer, firm == null ? "" : firm.getName(), safeSchedule(id));
    }

    private LawyerResponse toResponseWithoutSchedule(Lawyer lawyer) {
        LawFirm firm = lawFirmMapper.selectById(lawyer.getLawFirmId());
        return LawyerResponse.of(lawyer, firm == null ? "" : firm.getName(), List.of());
    }

    private List<ScheduleSlotResponse> safeSchedule(Long lawyerId) {
        try {
            return appointmentClient.schedule(lawyerId).getData();
        } catch (Exception ignored) {
            return defaultSchedule();
        }
    }

    private List<ScheduleSlotResponse> defaultSchedule() {
        LocalDate date = LocalDate.now();
        return java.util.stream.Stream.iterate(date.plusDays(1), day -> day.plusDays(1))
                .filter(day -> day.getDayOfWeek().getValue() <= 5)
                .limit(5)
                .map(day -> {
                    ScheduleSlotResponse slot = new ScheduleSlotResponse();
                    slot.setDate(day.toString());
                    slot.setStartTime(LocalDateTime.of(day, LocalTime.of(9, 0)).toString());
                    slot.setEndTime(LocalDateTime.of(day, LocalTime.of(10, 0)).toString());
                    slot.setAvailable(true);
                    return slot;
                })
                .toList();
    }
}
