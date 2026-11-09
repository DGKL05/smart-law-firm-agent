package com.example.dgkl.lawyer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.dgkl.common.exception.BusinessException;
import com.example.dgkl.common.response.ApiResponse;
import com.example.dgkl.common.response.PageResult;
import com.example.dgkl.lawyer.client.AppointmentClient;
import com.example.dgkl.lawyer.dto.LawFirmDetailResponse;
import com.example.dgkl.lawyer.dto.LawyerResponse;
import com.example.dgkl.lawyer.dto.ScheduleSlotResponse;
import com.example.dgkl.lawyer.entity.LawFirm;
import com.example.dgkl.lawyer.entity.Lawyer;
import com.example.dgkl.lawyer.mapper.LawFirmMapper;
import com.example.dgkl.lawyer.mapper.LawyerMapper;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PublicLawyerService {
    private final LawFirmMapper lawFirmMapper;
    private final LawyerMapper lawyerMapper;
    private final AppointmentClient appointmentClient;
    private final PublicDirectoryCache directoryCache;

    public PageResult<LawFirm> lawFirms(int pageNum, int pageSize, String provinceCodes, String keyword) {
        LambdaQueryWrapper<LawFirm> query = new LambdaQueryWrapper<LawFirm>()
                .eq(LawFirm::getDeleted, 0)
                .orderByDesc(LawFirm::getId);
        if (StringUtils.hasText(provinceCodes)) {
            List<String> codes = Arrays.stream(provinceCodes.split(","))
                    .map(String::strip)
                    .filter(StringUtils::hasText)
                    .toList();
            query.in(!codes.isEmpty(), LawFirm::getProvinceCode, codes);
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
        LawFirm firm = directoryCache.lawFirm(id);
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

    public PageResult<LawyerResponse> lawyers(int pageNum, int pageSize, String category, Long lawFirmId,
                                              String keyword) {
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
        Lawyer lawyer = directoryCache.lawyer(id);
        if (lawyer == null || Integer.valueOf(1).equals(lawyer.getDeleted())) {
            throw new BusinessException("律师不存在");
        }
        LawFirm firm = directoryCache.lawFirm(lawyer.getLawFirmId());
        return LawyerResponse.of(lawyer, firm == null ? "" : firm.getName(), safeSchedule(id));
    }

    private LawyerResponse toResponseWithoutSchedule(Lawyer lawyer) {
        LawFirm firm = lawFirmMapper.selectById(lawyer.getLawFirmId());
        return LawyerResponse.of(lawyer, firm == null ? "" : firm.getName(), List.of());
    }

    private List<ScheduleSlotResponse> safeSchedule(Long lawyerId) {
        try {
            ApiResponse<List<ScheduleSlotResponse>> response = appointmentClient.schedule(lawyerId);
            if (response != null && response.getCode() == 200 && response.getData() != null) {
                return response.getData();
            }
        } catch (FeignException exception) {
            log.warn("查询律师排班失败，lawyerId={}，下游状态={}", lawyerId, exception.status());
        }
        return List.of();
    }
}
