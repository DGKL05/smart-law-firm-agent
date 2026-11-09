package com.example.dgkl.lawyer.service.impl;

import com.example.dgkl.lawyer.entity.LawyerMedia;
import com.example.dgkl.lawyer.mapper.LawyerMediaMapper;
import com.example.dgkl.lawyer.service.ImageUploadValidator;
import com.example.dgkl.lawyer.service.LawyerMediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LawyerMediaServiceImpl implements LawyerMediaService {
    private static final String PUBLIC_URL_PREFIX = "/api/public/media/";

    private final ImageUploadValidator validator;
    private final LawyerMediaMapper mediaMapper;

    @Override
    public String replace(String previousUrl, MultipartFile file) {
        ImageUploadValidator.ValidatedImage image = validator.validate(file);
        LawyerMedia media = new LawyerMedia();
        media.setId(UUID.randomUUID().toString());
        media.setContentType(image.contentType());
        media.setImageData(image.data());
        mediaMapper.insert(media);
        remove(previousUrl);
        return PUBLIC_URL_PREFIX + media.getId();
    }

    @Override
    public void remove(String url) {
        if (url != null && url.startsWith(PUBLIC_URL_PREFIX)) {
            mediaMapper.deleteById(url.substring(PUBLIC_URL_PREFIX.length()));
        }
    }

    @Override
    public LawyerMedia find(UUID id) {
        return mediaMapper.selectById(id.toString());
    }
}
