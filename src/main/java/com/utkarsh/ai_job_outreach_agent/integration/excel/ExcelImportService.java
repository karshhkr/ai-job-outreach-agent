package com.utkarsh.ai_job_outreach_agent.integration.excel;

import com.utkarsh.ai_job_outreach_agent.entity.HrContact;
import com.utkarsh.ai_job_outreach_agent.repository.HrContactRepository;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.*;

@Service
public class ExcelImportService {
    private final HrContactRepository repository;
    public ExcelImportService(HrContactRepository repository) { this.repository = repository; }

    @Transactional
    public Map<String, Object> importContacts(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Upload an .xlsx file");
        String filename = Objects.toString(file.getOriginalFilename(), "").toLowerCase(Locale.ROOT);
        if (!filename.endsWith(".xlsx")) throw new IllegalArgumentException("Only .xlsx files are supported");
        int imported = 0, skipped = 0;
        List<String> errors = new ArrayList<>();
        try (InputStream in = file.getInputStream(); Workbook workbook = WorkbookFactory.create(in)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet.getPhysicalNumberOfRows() < 2) throw new IllegalArgumentException("Excel must contain a header row and at least one data row");
            DataFormatter formatter = new DataFormatter();
            Map<String, Integer> columns = new HashMap<>();
            Row header = sheet.getRow(0);
            for (Cell cell : header) columns.put(formatter.formatCellValue(cell).trim().toLowerCase(Locale.ROOT), cell.getColumnIndex());
            if (!columns.containsKey("name") || !columns.containsKey("email"))
                throw new IllegalArgumentException("Required Excel columns: name, email. Optional: company, jobtitle, linkedinurl, notes");

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) { skipped++; continue; }
                String name = value(row, columns, "name", formatter);
                String email = value(row, columns, "email", formatter).toLowerCase(Locale.ROOT);
                if (name.isBlank() || email.isBlank() || !email.contains("@")) {
                    skipped++; errors.add("Row " + (i + 1) + ": name/email missing or invalid"); continue;
                }
                if (repository.findByEmailIgnoreCase(email).isPresent()) { skipped++; continue; }
                HrContact c = new HrContact();
                c.setName(name); c.setEmail(email);
                c.setCompany(value(row, columns, "company", formatter));
                c.setJobTitle(value(row, columns, "jobtitle", formatter));
                c.setLinkedinUrl(value(row, columns, "linkedinurl", formatter));
                c.setNotes(value(row, columns, "notes", formatter));
                repository.save(c); imported++;
            }
        }
        return Map.of("imported", imported, "skipped", skipped, "errors", errors);
    }

    private String value(Row row, Map<String, Integer> columns, String key, DataFormatter formatter) {
        Integer index = columns.get(key);
        if (index == null) return "";
        Cell cell = row.getCell(index);
        return cell == null ? "" : formatter.formatCellValue(cell).trim();
    }
}
