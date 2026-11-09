package com.example.dgkl.lawyer.service;

import com.example.dgkl.common.exception.BusinessException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;

@Component
public class ImageUploadValidator {
    private static final int MAX_BYTES = 2 * 1024 * 1024;
    private static final int MAX_DIMENSION = 4096;
    private static final long MAX_PIXELS = 4_000_000L;

    public ValidatedImage validate(MultipartFile file) {
        if (file.isEmpty() || file.getSize() > MAX_BYTES) {
            throw new BusinessException("请选择不超过 2 MiB 的图片");
        }
        String filename = file.getOriginalFilename();
        if (filename != null && (filename.contains("/") || filename.contains("\\") || filename.indexOf('\0') >= 0)) {
            throw new BusinessException("图片文件名不能包含路径");
        }
        try {
            byte[] data = file.getBytes();
            if (data.length > MAX_BYTES) {
                throw new BusinessException("图片大小不能超过 2 MiB");
            }
            return decodeAndReencode(data);
        } catch (IOException | IllegalArgumentException exception) {
            throw new BusinessException("无法读取图片，请上传有效的 JPEG 或 PNG 文件");
        }
    }

    private ValidatedImage decodeAndReencode(byte[] data) throws IOException {
        try (MemoryCacheImageInputStream input = new MemoryCacheImageInputStream(new ByteArrayInputStream(data))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new BusinessException("只支持 JPEG 或 PNG 图片");
            }
            ImageReader reader = readers.next();
            try {
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                if (!"jpeg".equals(format) && !"png".equals(format)) {
                    throw new BusinessException("只支持 JPEG 或 PNG 图片");
                }
                reader.setInput(input, true, true);
                checkDimensions(reader.getWidth(0), reader.getHeight(0));
                return encode(reader.read(0), format);
            } finally {
                reader.dispose();
            }
        }
    }

    private void checkDimensions(int width, int height) {
        if (width < 1 || height < 1 || width > MAX_DIMENSION || height > MAX_DIMENSION
                || (long) width * height > MAX_PIXELS) {
            throw new BusinessException("图片宽高不能超过 4096 像素，总像素不能超过 400 万");
        }
    }

    private ValidatedImage encode(BufferedImage image, String format) throws IOException {
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        try (MemoryCacheImageOutputStream output = new MemoryCacheImageOutputStream(data)) {
            if (!ImageIO.write(image, format, output)) {
                throw new BusinessException("无法保存图片");
            }
        }
        if (data.size() > MAX_BYTES) {
            throw new BusinessException("处理后的图片超过 2 MiB，请缩小图片后重试");
        }
        return new ValidatedImage("image/" + format, data.toByteArray());
    }

    public record ValidatedImage(String contentType, byte[] data) {
    }
}
