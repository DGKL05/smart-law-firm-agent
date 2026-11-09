package com.example.dgkl.lawyer.service;

import com.example.dgkl.common.response.PageResult;
import com.example.dgkl.lawyer.dto.LawyerAdminRequest;
import com.example.dgkl.lawyer.dto.LawyerProfileRequest;
import com.example.dgkl.lawyer.dto.LawyerResponse;
import com.example.dgkl.lawyer.dto.LawyerSnapshotResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface LawyerService {
    PageResult<LawyerResponse> list(int pageNum, int pageSize, String keyword, Long lawFirmId);

    LawyerResponse get(Long id);

    LawyerResponse create(LawyerAdminRequest request);

    LawyerResponse update(Long id, LawyerAdminRequest request);

    void delete(Long id);

    LawyerResponse getProfile(Long userId);

    LawyerResponse updateProfile(Long userId, LawyerProfileRequest request);

    String uploadAvatar(Long id, MultipartFile file);

    String uploadProfileAvatar(Long userId, MultipartFile file);

    LawyerSnapshotResponse snapshot(Long id);

    LawyerSnapshotResponse snapshotByUserId(Long userId);

    Map<String, Long> stats();
}
