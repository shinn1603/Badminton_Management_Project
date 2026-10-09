package vn.yain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Cloudinary Service Unit Tests")
class CloudinaryServiceTest {

    private CloudinaryService cloudinaryService;

    @BeforeEach
    void setUp() {
        cloudinaryService = new CloudinaryService("test-cloud", "test-key", "test-secret");
    }

    @Test
    @DisplayName("Upload image succeeds and returns CDN URL")
    void testUploadImageSuccess() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "court-vip.jpg",
                "image/jpeg",
                "dummy image content".getBytes()
        );

        String url = cloudinaryService.uploadImage(file, "courts");
        assertNotNull(url);
        assertTrue(url.contains("cloudinary.com"));
        assertTrue(url.contains("court-vip.jpg"));
    }

    @Test
    @DisplayName("Upload null or empty image throws IllegalArgumentException")
    void testUploadEmptyImageThrowsException() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.jpg",
                "image/jpeg",
                new byte[0]
        );

        assertThrows(IllegalArgumentException.class, () -> {
            cloudinaryService.uploadImage(emptyFile, "products");
        });

        assertThrows(IllegalArgumentException.class, () -> {
            cloudinaryService.uploadImage(null, "products");
        });
    }

    @Test
    @DisplayName("Delete image returns boolean status")
    void testDeleteImage() {
        boolean result = cloudinaryService.deleteImage("courts/test-id");
        assertNotNull(result);
    }
}
