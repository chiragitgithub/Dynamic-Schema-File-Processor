package com.example.fileprocessor.controller;

import com.example.fileprocessor.service.ExportService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@CrossOrigin(origins = "http://localhost:63342") //
@RestController
@RequestMapping("/api")
public class ExportController {

    private final ExportService exportService;

    @Autowired
    public ExportController(ExportService exportService) {
        this.exportService = exportService;
    }


    @GetMapping("/export")
    public void exportData(
            @RequestParam String tableName,
            @RequestParam(required = false) String id,
            @RequestParam(required = false, defaultValue = "id") String primaryKey,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "csv") String format,
            @RequestParam Map<String, String> allParams,
            HttpServletResponse response
    ) throws IOException {

        Map<String, String> filters = new HashMap<>(allParams);
        filters.keySet().removeIf(key ->
                key.equals("tableName") ||
                        key.equals("id") ||
                        key.equals("primaryKey") ||
                        key.equals("page") ||
                        key.equals("size") ||
                        key.equals("format")
        );


        System.out.println("Export Request");
        System.out.println("Table: " + tableName);
        System.out.println("IDs: " + id);
        System.out.println("Primary Key: " + primaryKey);
        System.out.println("Filters: " + filters);
        System.out.println("Page: " + page + ", Size: " + size + ", Format: " + format);

        try {
            exportService.exportData(tableName, id, primaryKey, page, size, format, filters, response);
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("text/plain");
            response.getWriter().write("Export failed: " + e.getMessage());
        }
    }
}
