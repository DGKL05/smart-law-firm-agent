package com.example.dgkl.module.publicapi.controller;

import com.example.dgkl.common.PageResult;
import com.example.dgkl.common.Result;
import com.example.dgkl.module.lawfirm.entity.LawFirm;
import com.example.dgkl.module.publicapi.dto.PublicLawFirmDetailResponse;
import com.example.dgkl.module.publicapi.dto.PublicLawyerDetailResponse;
import com.example.dgkl.module.publicapi.dto.PublicLawyerResponse;
import com.example.dgkl.module.publicapi.service.PublicQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicController {
    private final PublicQueryService publicQueryService;

    @GetMapping("/law-firms")
    public Result<PageResult<LawFirm>> lawFirms(@RequestParam(defaultValue = "1") long pageNum,
                                                @RequestParam(defaultValue = "10") long pageSize,
                                                @RequestParam(required = false) String provinceCode,
                                                @RequestParam(required = false) String provinceCodes,
                                                @RequestParam(required = false) String keyword) {
        return Result.success(publicQueryService.lawFirms(pageNum, pageSize, provinceCode, provinceCodes, keyword));
    }

    @GetMapping("/law-firms/{id}")
    public Result<PublicLawFirmDetailResponse> lawFirm(@PathVariable Long id) {
        return Result.success(publicQueryService.lawFirm(id));
    }

    @GetMapping("/lawyers")
    public Result<PageResult<PublicLawyerResponse>> lawyers(@RequestParam(defaultValue = "1") long pageNum,
                                                            @RequestParam(defaultValue = "10") long pageSize,
                                                            @RequestParam(required = false) String category,
                                                            @RequestParam(required = false) Long lawFirmId,
                                                            @RequestParam(required = false) String keyword) {
        return Result.success(publicQueryService.lawyers(pageNum, pageSize, category, lawFirmId, keyword));
    }

    @GetMapping("/lawyers/{id}")
    public Result<PublicLawyerDetailResponse> lawyer(@PathVariable Long id) {
        return Result.success(publicQueryService.lawyer(id));
    }

    @GetMapping("/home/stats")
    public Result<Map<String, Long>> homeStats() {
        return Result.success(publicQueryService.homeStats());
    }
}
