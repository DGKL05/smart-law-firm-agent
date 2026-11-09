package com.example.dgkl.lawyer.service;

import com.example.dgkl.common.response.PageResult;
import com.example.dgkl.lawyer.dto.LawFirmRequest;
import com.example.dgkl.lawyer.entity.LawFirm;
import org.springframework.web.multipart.MultipartFile;

public interface LawFirmService {
    PageResult<LawFirm> list(int pageNum, int pageSize, String keyword);

    LawFirm get(Long id);

    LawFirm create(LawFirmRequest request);

    LawFirm update(Long id, LawFirmRequest request);

    void delete(Long id);

    String uploadPhoto(Long id, MultipartFile file);
}
