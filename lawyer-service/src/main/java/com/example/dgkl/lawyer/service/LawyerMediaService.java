package com.example.dgkl.lawyer.service;

import com.example.dgkl.lawyer.entity.LawyerMedia;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface LawyerMediaService {
    String replace(String previousUrl, MultipartFile file);

    void remove(String url);

    LawyerMedia find(UUID id);
}
