
package com.example.fileprocessor.parser.impl;

import com.example.fileprocessor.model.ColumnSchema;
import com.example.fileprocessor.model.FileSchema;
import com.example.fileprocessor.model.ValidationError;
import com.example.fileprocessor.parser.FileParser;
import com.example.fileprocessor.validator.impl.BasicFieldValidator;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
public class CsvFileParser implements FileParser {

    private static final int DEFAULT_VARCHAR_LIMIT = 255;

    @Override
    public List<Map<String, Object>> parse(MultipartFile file, FileSchema schema,
                                           List<ValidationError> errors, char delimiter) throws Exception {

        List<Map<String, Object>> result = new ArrayList<>();

        String cleanedContent = sanitizeFile(file);

        try (Reader reader = new StringReader(cleanedContent)) {

            CSVFormat csvFormat = CSVFormat.EXCEL
                    .withDelimiter(delimiter)
                    .withQuote('"')
                    .withEscape('\\')
                    .withIgnoreSurroundingSpaces()
                    .withIgnoreEmptyLines()
                    .withAllowMissingColumnNames()
                    .withFirstRecordAsHeader()
                    .withRecordSeparator('\n');

            CSVParser parser = new CSVParser(reader, csvFormat);

            int rowNum = 1;
            Map<String, ColumnSchema> columnSchemas = schema.getColumns();

            for (CSVRecord record : parser) {
                Map<String, Object> row = new HashMap<>();

                for (Map.Entry<String, ColumnSchema> entry : columnSchemas.entrySet()) {
                    String dbField = entry.getKey();
                    ColumnSchema colSchema = entry.getValue();
                    String csvHeader = colSchema.getCsvColumn();

                    String rawValue;
                    try {
                        rawValue = record.get(csvHeader);
                    } catch (IllegalArgumentException ex) {
                        errors.add(new ValidationError(rowNum, dbField,
                                "CSV header '" + csvHeader + "' not found"));
                        continue;
                    }

                    try {
                        Object transformed = transformAndConvertWithIsoSupport(
                                rawValue, colSchema.getType(), colSchema.getTransform()
                        );

                        if ("string".equalsIgnoreCase(colSchema.getType()) && transformed != null) {
                            String strVal = transformed.toString();
                            if (strVal.length() > DEFAULT_VARCHAR_LIMIT) {
                                strVal = strVal.substring(0, DEFAULT_VARCHAR_LIMIT);
                            }
                            row.put(dbField, strVal);
                        } else {
                            row.put(dbField, transformed);
                        }

                    } catch (Exception ex) {
                        errors.add(new ValidationError(rowNum, dbField, ex.getMessage()));
                    }
                }

                result.add(row);
                rowNum++;
            }

        } catch (Exception ex) {
            throw new RuntimeException("Failed to parse CSV: " + ex.getMessage(), ex);
        }

        return result;
    }

    private String sanitizeFile(MultipartFile file) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Remove non-printable/control characters except tab/newline
                line = line.replaceAll("[\\p{C}&&[^\t\n]]", "");
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }

    private Object transformAndConvertWithIsoSupport(String rawValue, String type, String transform) {
        try {
            return BasicFieldValidator.transformAndConvert(rawValue, type, transform);
        } catch (Exception ex) {
            // Fallback for ISO 8601 datetime parsing
            if (type != null && (type.equalsIgnoreCase("date") || type.equalsIgnoreCase("datetime"))) {
                try {
                    OffsetDateTime isoParsed = OffsetDateTime.parse(rawValue, DateTimeFormatter.ISO_DATE_TIME);
                    return isoParsed;
                } catch (Exception ignored) {
                    // Ignore, throw below
                }
            }
            throw new RuntimeException("Unparseable " + type + ": \"" + rawValue + "\"");
        }
    }
}
