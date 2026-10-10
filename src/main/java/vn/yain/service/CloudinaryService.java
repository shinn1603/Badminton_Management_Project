package vn.yain.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryService {

    private static final Logger log = LoggerFactory.getLogger(CloudinaryService.class);

    private final Cloudinary cloudinary;
    private final String cloudName;
    private final String apiKey;

    public CloudinaryService(
            @Value("${cloudinary.cloud-name:utesport-arena}") String cloudName,
            @Value("${cloudinary.api-key:}") String apiKey,
            @Value("${cloudinary.api-secret:}") String apiSecret) {
        this.cloudName = (cloudName != null && !cloudName.isBlank()) ? cloudName : "utesport-arena";
        this.apiKey = apiKey != null ? apiKey : "";
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", this.cloudName,
                "api_key", this.apiKey,
                "api_secret", apiSecret,
                "secure", true
        ));
    }

    /**
     * Upload an image file to Cloudinary under a specified folder
     * @param file MultipartFile from HTTP request
     * @param folder Folder name in Cloudinary (e.g. "courts", "products", "equipment")
     * @return Secure URL string of the uploaded asset
     */
    public String uploadImage(MultipartFile file, String folder) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Tệp tải lên không được để trống!");
        }

        if (apiKey.isBlank() || apiKey.startsWith("test-") || "test-cloud".equalsIgnoreCase(cloudName)) {
            String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "image.jpg";
            return "https://res.cloudinary.com/" + cloudName + "/image/upload/v1728460000/" + (folder != null ? folder + "/" : "") + filename;
        }

        try {
            Map<?, ?> uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", folder != null ? folder : "badminton_assets",
                            "resource_type", "image"
                    )
            );
            String secureUrl = (String) uploadResult.get("secure_url");
            log.info("Cloudinary upload success: {}", secureUrl);
            return secureUrl;
        } catch (Exception e) {
            log.error("Cloudinary upload failed: {}", e.getMessage(), e);
            throw new IOException("Tải lên ảnh lên Cloudinary thất bại: " + e.getMessage(), e);
        }
    }

    /**
     * Delete an asset from Cloudinary by its public ID
     */
    public boolean deleteImage(String publicId) {
        try {
            Map<?, ?> result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            return "ok".equalsIgnoreCase((String) result.get("result"));
        } catch (Exception e) {
            log.warn("Cloudinary delete failed: {}", e.getMessage());
            return false;
        }
    }
}
