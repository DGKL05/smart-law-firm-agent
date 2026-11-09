package com.example.dgkl.lawyer.controller;

import com.example.dgkl.lawyer.entity.LawyerMedia;
import com.example.dgkl.lawyer.service.LawyerMediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/public/media")
@RequiredArgsConstructor
public class PublicMediaController {
    private final LawyerMediaService mediaService;

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> image(@PathVariable UUID id) {
        LawyerMedia media = mediaService.find(id);
        if (media == null) {
            return ResponseEntity.notFound().build();
        }
        byte[] imageData = media.getImageData();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(media.getContentType()))
                .contentLength(imageData.length)
                .cacheControl(CacheControl.noCache())
                .header("X-Content-Type-Options", "nosniff")
                .body(imageData);
    }
}
