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
}
