package com.project.ridemesh.client;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;
    private final String uploadPreset = "RideMeshCloudinaryBucket";

    public CloudinaryService() {
        this.cloudinary = new Cloudinary("cloudinary://791693843498433:ya7ywjxa_NOr8D-Ra6VlR3W8oT0@dvb5mesnd");
    }

    @SuppressWarnings("unchecked")
    public String uploadImage(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }
        Map<String, Object> params = ObjectUtils.asMap(
            "upload_preset", uploadPreset
        );
        Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), params);
        return (String) uploadResult.get("secure_url");
    }

    @SuppressWarnings("unchecked")
    public String uploadFromUrl(String remoteUrl) {
        if (remoteUrl == null || remoteUrl.trim().isEmpty()) {
            return null;
        }
        try {
            Map<String, Object> params = ObjectUtils.asMap(
                "upload_preset", uploadPreset
            );
            Map<?, ?> uploadResult = cloudinary.uploader().upload(remoteUrl, params);
            if (uploadResult != null && uploadResult.containsKey("secure_url")) {
                return (String) uploadResult.get("secure_url");
            }
        } catch (Exception e) {
            try {
                Map<?, ?> uploadResult = cloudinary.uploader().upload(remoteUrl, ObjectUtils.emptyMap());
                if (uploadResult != null && uploadResult.containsKey("secure_url")) {
                    return (String) uploadResult.get("secure_url");
                }
            } catch (Exception ex) {
                System.err.println("Failed to upload remote URL to Cloudinary: " + ex.getMessage());
            }
        }
        return null;
    }
}
