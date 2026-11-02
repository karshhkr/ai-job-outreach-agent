
package com.utkarsh.ai_job_outreach_agent.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path uploadPath;

    public FileStorageService(
            @Value("${app.upload-dir:./uploads}") String uploadDir
    ) {
        this.uploadPath = Paths.get(uploadDir)
                .toAbsolutePath()
                .normalize();

        try {
            Files.createDirectories(this.uploadPath);
        } catch (IOException exception) {
            throw new RuntimeException(
                    "Could not create upload directory",
                    exception
            );
        }
    }

    public String saveResume(MultipartFile file) throws IOException {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Resume file cannot be empty");
        }

        String originalFilename = file.getOriginalFilename();

        if (originalFilename == null ||
                !originalFilename.toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("Only PDF resumes are allowed");
        }

        String filename = UUID.randomUUID() + ".pdf";

        Path targetPath = uploadPath.resolve(filename).normalize();

        if (!targetPath.startsWith(uploadPath)) {
            throw new IllegalArgumentException("Invalid file path");
        }

        Files.copy(file.getInputStream(), targetPath);

        return targetPath.toString();
    }
}
