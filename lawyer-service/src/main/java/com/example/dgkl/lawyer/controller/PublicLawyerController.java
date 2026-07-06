package com.example.dgkl.lawyer.controller;

import com.example.dgkl.common.response.ApiResponse;
import com.example.dgkl.common.response.PageResult;
import com.example.dgkl.lawyer.dto.LawFirmDetailResponse;
import com.example.dgkl.lawyer.dto.LawyerResponse;
import com.example.dgkl.lawyer.entity.LawFirm;
import com.example.dgkl.lawyer.service.PublicLawyerService;
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
    private final PublicLawyerService publicLawyerService;

    @GetMapping("/law-firms")
    public ApiResponse<PageResult<LawFirm>> lawFirms(@RequestParam(defaultValue = "1") int pageNum,
                                                     @RequestParam(defaultValue = "10") int pageSize,
                                                     @RequestParam(required = false) String provinceCodes,
                                                     @RequestParam(required = false) String keyword) {
        return ApiResponse.success(publicLawyerService.lawFirms(pageNum, pageSize, provinceCodes, keyword));
    }

    @GetMapping("/law-firms/{id}")
    public ApiResponse<LawFirmDetailResponse> lawFirmDetail(@PathVariable Long id) {
        return ApiResponse.success(publicLawyerService.lawFirmDetail(id));
    }

    @GetMapping("/lawyers")
    public ApiResponse<PageResult<LawyerResponse>> lawyers(@RequestParam(defaultValue = "1") int pageNum,
                                                           @RequestParam(defaultValue = "10") int pageSize,
                                                           @RequestParam(required = false) String category,
                                                           @RequestParam(required = false) Long lawFirmId,
                                                           @RequestParam(required = false) String keyword) {
        return ApiResponse.success(publicLawyerService.lawyers(pageNum, pageSize, category, lawFirmId, keyword));
    }

    @GetMapping("/lawyers/{id}")
    public ApiResponse<LawyerResponse> lawyerDetail(@PathVariable Long id) {
        return ApiResponse.success(publicLawyerService.lawyerDetail(id));
    }
}
