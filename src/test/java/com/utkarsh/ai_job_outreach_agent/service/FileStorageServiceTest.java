
package com.utkarsh.ai_job_outreach_agent.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FileStorageServiceTest {

    @TempDir
    Path tempDir;

    private FileStorageService fileStorageService;

    @BeforeEach
    void setUp() {
        fileStorageService = new FileStorageService(
                tempDir.resolve("uploads").toString()
        );
    }

    // 1. Save PDF successfully
    @Test
    void saveResume_shouldSavePdfSuccessfully() throws IOException {

        byte[] content = "%PDF-1.4 test resume".getBytes();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "resume.pdf",
                "application/pdf",
                content
        );

        String savedPath = fileStorageService.saveResume(file);

        Path path = Path.of(savedPath);

        assertTrue(Files.exists(path));
        assertTrue(savedPath.endsWith(".pdf"));
        assertArrayEquals(content, Files.readAllBytes(path));
    }

    // 2. Saved filename should be unique
    @Test
    void saveResume_shouldGenerateUniqueFilename() throws IOException {

        MockMultipartFile file1 = new MockMultipartFile(
                "file", "resume.pdf", "application/pdf", "Resume 1".getBytes()
        );

        MockMultipartFile file2 = new MockMultipartFile(
                "file", "resume.pdf", "application/pdf", "Resume 2".getBytes()
        );

        String path1 = fileStorageService.saveResume(file1);
        String path2 = fileStorageService.saveResume(file2);

        assertNotEquals(path1, path2);
    }

    // 3. Null file should throw exception
    @Test
    void saveResume_shouldThrowException_whenFileIsNull() {

        assertThrows(
                IllegalArgumentException.class,
                () -> fileStorageService.saveResume(null)
        );
    }

    // 4. Empty file should throw exception
    @Test
    void saveResume_shouldThrowException_whenFileIsEmpty() {

        MockMultipartFile file = new MockMultipartFile(
                "file", "resume.pdf", "application/pdf", new byte[0]
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> fileStorageService.saveResume(file)
        );
    }

    // 5. Non-PDF file should throw exception
    @Test
    void saveResume_shouldThrowException_whenFileIsNotPdf() {

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "resume.docx",
                "application/pdf",
                "Some document".getBytes()
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> fileStorageService.saveResume(file)
        );

        assertEquals("Only PDF resumes are allowed", exception.getMessage());
    }

    // 6. Filename extension should be case-insensitive
    @Test
    void saveResume_shouldAcceptUppercasePdfExtension() throws IOException {

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "RESUME.PDF",
                "application/pdf",
                "PDF content".getBytes()
        );

        String savedPath = fileStorageService.saveResume(file);

        assertTrue(Files.exists(Path.of(savedPath)));
    }

    // 7. Missing filename should throw exception
    @Test
    void saveResume_shouldThrowException_whenFilenameIsNull() {

        MultipartFile file = new MockMultipartFile(
                "file",
                null,
                "application/pdf",
                "PDF content".getBytes()
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> fileStorageService.saveResume(file)
        );
    }

    // 8. Upload directory should be created automatically
    @Test
    void constructor_shouldCreateUploadDirectory() {

        Path uploadDirectory = tempDir.resolve("uploads");

        assertTrue(Files.exists(uploadDirectory));
        assertTrue(Files.isDirectory(uploadDirectory));
    }
}
