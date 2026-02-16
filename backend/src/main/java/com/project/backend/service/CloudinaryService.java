package com.project.backend.service;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;

@Service
public class CloudinaryService {

    @Autowired
    private Cloudinary cloudinary;
    
    @Value("${cloudinary.folder:products}")
    private String defaultFolder;

    /**
     * Upload image to Cloudinary
     */
    public Map uploadImage(MultipartFile multipartFile) throws IOException {
        return uploadImage(multipartFile, defaultFolder);
    }

    /**
     * Upload image to Cloudinary with specific folder
     */
    public Map uploadImage(MultipartFile multipartFile, String folder) throws IOException {
        try {
            // Generate unique public ID
            String publicId = UUID.randomUUID().toString();
            
            // Upload options
            Map<String, Object> options = ObjectUtils.asMap(
                "folder", folder,
                "public_id", publicId,
                "overwrite", true,
                "resource_type", "auto"
            );
            
            // Upload file
            Map uploadResult = cloudinary.uploader().upload(multipartFile.getBytes(), options);
            
            return uploadResult;
            
        } catch (IOException e) {
            throw new IOException("Failed to upload image to Cloudinary", e);
        }
    }

    /**
     * Upload with transformations (auto-optimization)
     */
    public Map uploadOptimizedImage(MultipartFile multipartFile, Long productId) throws IOException {
        String folder = "products/" + productId;
        String publicId = UUID.randomUUID().toString();
        
        Map<String, Object> options = ObjectUtils.asMap(
            "folder", folder,
            "public_id", publicId,
            "transformation", new Transformation<>()
                .width(800).height(800).crop("limit")
                .quality("auto")
                .fetchFormat("auto")
        );
        
        return cloudinary.uploader().upload(multipartFile.getBytes(), options);
    }

    /**
     * Upload multiple sizes for responsive images
     */
    public Map<String, String> uploadResponsiveImage(MultipartFile file, Long productId) throws IOException {
        String folder = "products/" + productId;
        String basePublicId = UUID.randomUUID().toString();
        
        Map<String, String> urls = new java.util.HashMap<>();
        
        // Thumbnail (100x100)
        Map thumbResult = cloudinary.uploader().upload(file.getBytes(), 
            ObjectUtils.asMap(
                "folder", folder,
                "public_id", basePublicId + "_thumb",
                "transformation", new Transformation<>().width(100).height(100).crop("fill")
            ));
        urls.put("thumbnail", (String) thumbResult.get("secure_url"));
        
        // Medium (400x400)
        Map mediumResult = cloudinary.uploader().upload(file.getBytes(),
            ObjectUtils.asMap(
                "folder", folder,
                "public_id", basePublicId + "_medium",
                "transformation", new Transformation<>().width(400).height(400).crop("limit")
            ));
        urls.put("medium", (String) mediumResult.get("secure_url"));
        
        // Large (800x800)
        Map largeResult = cloudinary.uploader().upload(file.getBytes(),
            ObjectUtils.asMap(
                "folder", folder,
                "public_id", basePublicId + "_large",
                "transformation", new Transformation<>().width(800).height(800).crop("limit")
            ));
        urls.put("large", (String) largeResult.get("secure_url"));
        
        return urls;
    }

    /**
     * Delete image from Cloudinary
     */
    public Map deleteImage(String publicId) throws IOException {
        return cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
    }

    /**
     * Get image URL with transformations
     */
    public String getOptimizedImageUrl(String publicId, int width, int height) {
        return cloudinary.url()
            .transformation(new Transformation<>()
                .width(width).height(height).crop("fill")
                .quality("auto")
                .fetchFormat("auto"))
            .secure(true)
            .generate(publicId);
    }
}