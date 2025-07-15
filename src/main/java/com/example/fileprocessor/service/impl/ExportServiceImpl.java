package com.example.fileprocessor.service.impl;

import com.example.fileprocessor.service.ExportService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExportServiceImpl implements ExportService {

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void exportData(
            String tableName,
            String id,
            String primaryKey,
            int page,
            int size,
            String format,
            Map<String, String> filters,
            HttpServletResponse response
    ) throws IOException {
        try {
            StringBuilder query = new StringBuilder("SELECT * FROM " + quoteQualifiedName(tableName));
            MapSqlParameterSource paramMap = new MapSqlParameterSource();
            List<String> whereConditions = new ArrayList<>();


            if (id != null && !id.trim().isEmpty()) {
                List<Object> idList = Arrays.stream(id.split(","))
                        .map(String::trim)
                        .map(s -> {
                            try {
                                return Integer.parseInt(s);
                            } catch (NumberFormatException e) {
                                return s;
                            }
                        })
                        .collect(Collectors.toList());

                String key = (primaryKey == null || primaryKey.isBlank()) ? "id" : primaryKey.trim();
                List<String> placeholders = new ArrayList<>();

                for (int i = 0; i < idList.size(); i++) {
                    String paramName = "id" + i;
                    placeholders.add(":" + paramName);
                    paramMap.addValue(paramName, idList.get(i));
                }

                whereConditions.add(quoteIdentifier(key) + " IN (" + String.join(", ", placeholders) + ")");
            }


            if (filters != null && !filters.isEmpty()) {
                for (Map.Entry<String, String> entry : filters.entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    whereConditions.add(quoteIdentifier(key) + " = :" + key);
                    paramMap.addValue(key, value);
                }
            }


            if (!whereConditions.isEmpty()) {
                query.append(" WHERE ").append(String.join(" AND ", whereConditions));
            }


            query.append(" OFFSET :offset LIMIT :limit");
            paramMap.addValue("offset", page * size);
            paramMap.addValue("limit", size);


            List<Map<String, Object>> rows = namedParameterJdbcTemplate.queryForList(query.toString(), paramMap);


            switch (format.toLowerCase()) {
                case "json" -> exportAsJson(tableName, rows, response);
                case "excel" -> exportAsExcel(tableName, rows, response);
                default -> exportAsCsv(tableName, rows, response);
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("text/plain");
            response.getWriter().write("Export failed: " + e.getMessage());
        }
    }


    private String quoteIdentifier(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }


    private String quoteQualifiedName(String tableName) {
        return Arrays.stream(tableName.split("\\."))
                .map(this::quoteIdentifier)
                .reduce((a, b) -> a + "." + b)
                .orElse(quoteIdentifier(tableName));
    }


    private void exportAsJson(String tableName, List<Map<String, Object>> rows, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setHeader("Content-Disposition", "attachment; filename=" + tableName + "_export.json");
        response.getWriter().write(objectMapper.writeValueAsString(rows));
    }


    private void exportAsCsv(String tableName, List<Map<String, Object>> rows, HttpServletResponse response) throws IOException {
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=" + tableName + "_export.csv");

        try (PrintWriter writer = response.getWriter()) {
            if (!rows.isEmpty()) {
                List<String> headers = new ArrayList<>(rows.get(0).keySet());
                writer.println(String.join(",", headers));

                for (Map<String, Object> row : rows) {
                    List<String> values = headers.stream()
                            .map(header -> {
                                Object val = row.get(header);
                                return val == null ? "" : val.toString().replace(",", " ");
                            })
                            .collect(Collectors.toList());
                    writer.println(String.join(",", values));
                }
            } else {
                writer.println("No records found");
            }
        }
    }


    private void exportAsExcel(String tableName, List<Map<String, Object>> rows, HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=" + tableName + "_export.xlsx");

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Export");

            if (!rows.isEmpty()) {
                List<String> headers = new ArrayList<>(rows.get(0).keySet());

                Row headerRow = sheet.createRow(0);
                for (int i = 0; i < headers.size(); i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(headers.get(i));
                }

                for (int i = 0; i < rows.size(); i++) {
                    Map<String, Object> rowData = rows.get(i);
                    Row row = sheet.createRow(i + 1);
                    for (int j = 0; j < headers.size(); j++) {
                        Cell cell = row.createCell(j);
                        Object val = rowData.get(headers.get(j));
                        cell.setCellValue(val == null ? "" : val.toString());
                    }
                }

                for (int i = 0; i < headers.size(); i++) {
                    sheet.autoSizeColumn(i);
                }
            }

            try (ServletOutputStream out = response.getOutputStream()) {
                workbook.write(out);
                out.flush();
            }
        }
    }
}
