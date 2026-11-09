package com.example.dgkl.lawyer.controller;

import com.example.dgkl.common.response.ApiResponse;
import com.example.dgkl.common.response.PageResult;
import com.example.dgkl.lawyer.dto.ImageUploadResponse;
import com.example.dgkl.lawyer.dto.LawFirmRequest;
import com.example.dgkl.lawyer.entity.LawFirm;
import com.example.dgkl.lawyer.service.LawFirmService;
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
@RequestMapping("/api/admin/law-firms")
@RequiredArgsConstructor
public class AdminLawFirmController {
    private final LawFirmService lawFirmService;

    @GetMapping
    public ApiResponse<PageResult<LawFirm>> list(
            @RequestParam(defaultValue = "1") @Min(1) int pageNum,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int pageSize,
            @RequestParam(required = false) @Size(max = 128) String keyword) {
        return ApiResponse.success(lawFirmService.list(pageNum, pageSize, keyword));
    }

    @GetMapping("/{id}")
    public ApiResponse<LawFirm> get(@PathVariable @Positive Long id) {
        return ApiResponse.success(lawFirmService.get(id));
    }

    @PostMapping
    public ApiResponse<LawFirm> create(@Valid @RequestBody LawFirmRequest request) {
        return ApiResponse.success(lawFirmService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<LawFirm> update(@PathVariable @Positive Long id, @Valid @RequestBody LawFirmRequest request) {
        return ApiResponse.success(lawFirmService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable @Positive Long id) {
        lawFirmService.delete(id);
        return ApiResponse.success();
    }

    @PostMapping(value = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ImageUploadResponse> photo(@PathVariable @Positive Long id,
                                                  @RequestPart("file") MultipartFile file) {
        return ApiResponse.success(new ImageUploadResponse(lawFirmService.uploadPhoto(id, file)));
    }
}
