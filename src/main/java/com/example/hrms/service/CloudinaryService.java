package com.example.hrms.service;

import org.springframework.web.multipart.MultipartFile;

public interface CloudinaryService {

    UploadResult uploadImage(MultipartFile file, String folder);

    void deleteByPublicId(String publicId);

    record UploadResult(String url, String publicId) {}
}
