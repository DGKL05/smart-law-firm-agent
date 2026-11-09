package com.example.dgkl.lawyer.controller;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.EntryType;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.example.dgkl.common.exception.BusinessException;
import com.example.dgkl.common.response.ApiResponse;
import com.example.dgkl.common.response.PageResult;
import com.example.dgkl.lawyer.dto.LawFirmDetailResponse;
import com.example.dgkl.lawyer.dto.LawyerResponse;
import com.example.dgkl.lawyer.entity.LawFirm;
import com.example.dgkl.lawyer.service.PublicLawyerService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicLawyerController {
    static final String DETAIL_RESOURCE = "lawyer:public-detail";
    private final PublicLawyerService publicLawyerService;

    @GetMapping("/law-firms")
    public ApiResponse<PageResult<LawFirm>> lawFirms(
            @RequestParam(defaultValue = "1") @Min(1) int pageNum,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int pageSize,
            @RequestParam(required = false) @Size(max = 600) String provinceCodes,
            @RequestParam(required = false) @Size(max = 128) String keyword) {
        return ApiResponse.success(publicLawyerService.lawFirms(pageNum, pageSize, provinceCodes, keyword));
    }

    @GetMapping("/law-firms/{id}")
    public ApiResponse<LawFirmDetailResponse> lawFirmDetail(@PathVariable @Positive Long id) {
        return ApiResponse.success(publicLawyerService.lawFirmDetail(id));
    }

    @GetMapping("/lawyers")
    public ApiResponse<PageResult<LawyerResponse>> lawyers(
            @RequestParam(defaultValue = "1") @Min(1) int pageNum,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int pageSize,
            @RequestParam(required = false) @Size(max = 64) String category,
            @RequestParam(required = false) @Positive Long lawFirmId,
            @RequestParam(required = false) @Size(max = 128) String keyword) {
        return ApiResponse.success(publicLawyerService.lawyers(pageNum, pageSize, category, lawFirmId, keyword));
    }

    @GetMapping("/lawyers/{id}")
    public ApiResponse<LawyerResponse> lawyerDetail(@PathVariable @Positive Long id) {
        Entry entry = null;
        try {
            entry = SphU.entry(DETAIL_RESOURCE, EntryType.IN, 1, id);
            return ApiResponse.success(publicLawyerService.lawyerDetail(id));
        } catch (DegradeException exception) {
            throw new BusinessException(503, "律师详情暂时不可用，请稍后重试");
        } catch (BlockException exception) {
            throw new BusinessException(429, "查询过于频繁，请稍后重试");
        } finally {
            if (entry != null) {
                entry.exit(1, id);
            }
        }
    }
}
