package com.example.fileprocessor.schema.impl;

import com.example.fileprocessor.model.ColumnSchema;
import com.example.fileprocessor.model.FileSchema;
import com.example.fileprocessor.schema.SchemaGenerator;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
public class SchemaAutoGeneratorImpl implements SchemaGenerator {

    @Override
    public FileSchema generateSchema(MultipartFile file, String fileType, String tableName) throws Exception {
        CSVParser parser = CSVFormat.DEFAULT
                .withFirstRecordAsHeader()
                .parse(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));

        Map<String, ColumnSchema> columnSchemaMap = new LinkedHashMap<>();

        if (!parser.iterator().hasNext()) {
            throw new RuntimeException("CSV is empty");
        }

        CSVRecord firstRecord = parser.iterator().next();

        for (String header : parser.getHeaderNames()) {
            String sampleValue = firstRecord.get(header);
            String inferredType = inferType(sampleValue);

            ColumnSchema col = new ColumnSchema();
            col.setCsvColumn(header);
            col.setType(inferredType);
            columnSchemaMap.put(header.toLowerCase(), col);
        }

        FileSchema schema = new FileSchema();
        schema.setFileType(fileType);
        schema.setTableName(tableName);
        schema.setColumns(columnSchemaMap);

        return schema;
    }

    private String inferType(String value) {
        if (value == null || value.trim().isEmpty()) return "string";
        value = value.trim();
        try {
            Integer.parseInt(value);
            return "int";
        } catch (Exception ignored) {}
        try {
            Double.parseDouble(value);
            return "double";
        } catch (Exception ignored) {}
        return "string";
    }
}
