package com.example.fileprocessor.service;

import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

public interface ExportService {
    void exportData(String tableName,
                    String id,
                    String primaryKey,
                    int page,
                    int size,
                    String format,
                    Map<String, String> filters,
                    HttpServletResponse response) throws IOException;
}
