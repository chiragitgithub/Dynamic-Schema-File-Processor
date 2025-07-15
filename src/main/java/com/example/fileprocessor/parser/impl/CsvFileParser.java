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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
public class CsvFileParser implements FileParser {

    @Override
    public List<Map<String, Object>> parse(
            MultipartFile file,
            FileSchema schema,
            List<ValidationError> errors
    ) throws Exception {

        List<Map<String, Object>> result = new ArrayList<>();

        try (
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
                CSVParser parser = CSVFormat.DEFAULT
                        .withFirstRecordAsHeader()
                        .parse(reader)
        ) {
            String mode = Optional.ofNullable(schema.getMode()).orElse("TABLE").toUpperCase();
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
                        row.put(dbField, transformed);
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

    /**
     * A wrapper that handles ISO 8601 fallback for date/datetime fields.
     */
    private Object transformAndConvertWithIsoSupport(String rawValue, String type, String transform) {
        try {
            return BasicFieldValidator.transformAndConvert(rawValue, type, transform);
        } catch (Exception ex) {
            // If it's a date/datetime and the error was about parsing -> try ISO 8601
            if (type != null && (type.equalsIgnoreCase("date") || type.equalsIgnoreCase("datetime"))) {
                try {
                    OffsetDateTime isoParsed = OffsetDateTime.parse(rawValue, DateTimeFormatter.ISO_DATE_TIME);
                    return isoParsed;
                } catch (Exception ignored) {
                    // Fall through
                }
            }
            // If fallback also failed, rethrow original exception
            throw new RuntimeException("Unparseable " + type + ": \"" + rawValue + "\"");
        }
    }
}
