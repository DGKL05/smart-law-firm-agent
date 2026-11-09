package com.example.dgkl.lawyer.service;

import com.example.dgkl.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ImageUploadValidatorTest {
    private final ImageUploadValidator validator = new ImageUploadValidator();

    @Test
    void reencodesPngAndDropsAppendedContent() throws IOException {
        byte[] png = png(8, 6);
        byte[] suffix = "appended-script".getBytes(StandardCharsets.UTF_8);
        byte[] upload = Arrays.copyOf(png, png.length + suffix.length);
        System.arraycopy(suffix, 0, upload, png.length, suffix.length);

        ImageUploadValidator.ValidatedImage image = validator.validate(file("avatar.png", upload));

        assertEquals("image/png", image.contentType());
        assertEquals(8, ImageIO.read(new ByteArrayInputStream(image.data())).getWidth());
        assertFalse(new String(image.data(), StandardCharsets.ISO_8859_1).contains("appended-script"));
    }

    @Test
    void rejectsSvgDisguisedAsPng() {
        byte[] svg = "<svg xmlns='http://www.w3.org/2000/svg'/>".getBytes(StandardCharsets.UTF_8);

        assertThrows(BusinessException.class, () -> validator.validate(file("avatar.png", svg)));
    }

    @Test
    void rejectsFilenameWithPathAndOversizedImage() throws IOException {
        MockMultipartFile pathUpload = file("../avatar.png", png(8, 6));
        MockMultipartFile largeUpload = file("large.png", png(2001, 2001));

        assertThrows(BusinessException.class, () -> validator.validate(pathUpload));
        assertThrows(BusinessException.class, () -> validator.validate(largeUpload));
    }

    private MockMultipartFile file(String name, byte[] data) {
        return new MockMultipartFile("file", name, "image/png", data);
    }

    private byte[] png(int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }
}
