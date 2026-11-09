package com.example.dgkl.lawyer.controller;

import com.example.dgkl.common.response.ApiResponse;
import com.example.dgkl.common.security.SecurityUtils;
import com.example.dgkl.lawyer.dto.ImageUploadResponse;
import com.example.dgkl.lawyer.dto.LawyerProfileRequest;
import com.example.dgkl.lawyer.dto.LawyerResponse;
import com.example.dgkl.lawyer.service.LawyerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/lawyer-center")
@RequiredArgsConstructor
public class LawyerCenterController {
    private final LawyerService lawyerService;

    @GetMapping("/profile")
    public ApiResponse<LawyerResponse> profile() {
        return ApiResponse.success(lawyerService.getProfile(SecurityUtils.currentUserId()));
    }

    @PutMapping("/profile")
    public ApiResponse<LawyerResponse> updateProfile(@Valid @RequestBody LawyerProfileRequest request) {
        return ApiResponse.success(lawyerService.updateProfile(SecurityUtils.currentUserId(), request));
    }

    @PostMapping(value = "/profile/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ImageUploadResponse> avatar(@RequestPart("file") MultipartFile file) {
        String url = lawyerService.uploadProfileAvatar(SecurityUtils.currentUserId(), file);
        return ApiResponse.success(new ImageUploadResponse(url));
    }
}
