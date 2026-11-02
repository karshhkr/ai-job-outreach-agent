package com.utkarsh.ai_job_outreach_agent.controller;

import com.utkarsh.ai_job_outreach_agent.integration.excel.ExcelImportService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;

@RestController
@RequestMapping("/api/import")
public class ExcelController {
    private final ExcelImportService importer;
    public ExcelController(ExcelImportService importer) { this.importer = importer; }

    @PostMapping(value = "/excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> importExcel(@RequestParam("file") MultipartFile file) throws Exception {
        return importer.importContacts(file);
    }
}
