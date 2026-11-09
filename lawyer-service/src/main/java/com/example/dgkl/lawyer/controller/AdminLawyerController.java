package com.example.dgkl.lawyer.controller;

import com.example.dgkl.common.response.ApiResponse;
import com.example.dgkl.common.response.PageResult;
import com.example.dgkl.lawyer.dto.ImageUploadResponse;
import com.example.dgkl.lawyer.dto.LawyerAdminRequest;
import com.example.dgkl.lawyer.dto.LawyerResponse;
import com.example.dgkl.lawyer.service.LawyerService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/lawyers")
@RequiredArgsConstructor
public class AdminLawyerController {
    private final LawyerService lawyerService;

    @GetMapping
    public ApiResponse<PageResult<LawyerResponse>> list(
            @RequestParam(defaultValue = "1") @Min(1) int pageNum,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int pageSize,
            @RequestParam(required = false) @Size(max = 128) String keyword,
            @RequestParam(required = false) @Positive Long lawFirmId) {
        return ApiResponse.success(lawyerService.list(pageNum, pageSize, keyword, lawFirmId));
    }

    @GetMapping("/{id}")
    public ApiResponse<LawyerResponse> get(@PathVariable @Positive Long id) {
        return ApiResponse.success(lawyerService.get(id));
    }

    @PostMapping
    public ApiResponse<LawyerResponse> create(@Valid @RequestBody LawyerAdminRequest request) {
        return ApiResponse.success(lawyerService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<LawyerResponse> update(@PathVariable @Positive Long id,
                                             @Valid @RequestBody LawyerAdminRequest request) {
        return ApiResponse.success(lawyerService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable @Positive Long id) {
        lawyerService.delete(id);
        return ApiResponse.success();
    }

    @PostMapping(value = "/{id}/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ImageUploadResponse> avatar(@PathVariable @Positive Long id,
                                                   @RequestPart("file") MultipartFile file) {
        return ApiResponse.success(new ImageUploadResponse(lawyerService.uploadAvatar(id, file)));
    }
}
