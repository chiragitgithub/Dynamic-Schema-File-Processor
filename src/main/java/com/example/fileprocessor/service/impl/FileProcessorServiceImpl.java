package com.example.fileprocessor.service.impl;

import com.example.fileprocessor.model.ColumnSchema;
import com.example.fileprocessor.model.FileSchema;
import com.example.fileprocessor.model.ValidationError;
import com.example.fileprocessor.parser.FileParser;
import com.example.fileprocessor.schema.SchemaReader;
import com.example.fileprocessor.service.FileProcessorService;
import com.example.fileprocessor.util.DynamicTableCreator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class FileProcessorServiceImpl implements FileProcessorService {

    private final SchemaReader schemaReader;
    private final FileParser fileParser;
    private final JdbcTemplate jdbcTemplate;
    private final DynamicTableCreator dynamicTableCreator;

    public FileProcessorServiceImpl(SchemaReader schemaReader,
                                    FileParser fileParser,
                                    JdbcTemplate jdbcTemplate,
                                    DynamicTableCreator dynamicTableCreator) {
        this.schemaReader = schemaReader;
        this.fileParser = fileParser;
        this.jdbcTemplate = jdbcTemplate;
        this.dynamicTableCreator = dynamicTableCreator;
    }

    @Override
    public List<ValidationError> processFile(MultipartFile file, String fileType, String mode) throws Exception {
        System.out.println("Processing file with mode: " + mode);
        if (!"TABLE".equalsIgnoreCase(mode) && !"DYNAMIC".equalsIgnoreCase(mode)) {
            System.out.println("Invalid mode detected: " + mode);
            throw new IllegalArgumentException("Invalid mode. Allowed values are: TABLE or DYNAMIC");
        }

        FileSchema schema = schemaReader.readSchema(fileType);
        List<ValidationError> errors = new ArrayList<>();
        List<Map<String, Object>> rows = fileParser.parse(file, schema, errors);

        if ("DYNAMIC".equalsIgnoreCase(mode)) {
            dynamicTableCreator.createDynamicTableIfNotExists(fileType, schema);
        }

        for (int i = 0; i < rows.size(); i++) {
            if (!hasErrorForRow(errors, i + 1)) {
                if ("DYNAMIC".equalsIgnoreCase(mode)) {
                    insertOrUpdateRow(fileType, rows.get(i), schema);
                } else {
                    insertRow(schema.getTableName(), rows.get(i));
                }
            }
        }

        return errors;
    }

    private boolean hasErrorForRow(List<ValidationError> errors, int rowNum) {
        return errors.stream().anyMatch(e -> e.getRowNumber() == rowNum);
    }

    private void insertRow(String tableName, Map<String, Object> row) {
        StringBuilder columns = new StringBuilder();
        StringBuilder placeholders = new StringBuilder();
        List<Object> values = new ArrayList<>();

        for (Map.Entry<String, Object> entry : row.entrySet()) {
            columns.append("\"").append(entry.getKey()).append("\",");
            placeholders.append("?,");

            Object value = entry.getValue();

            if (value instanceof OffsetDateTime offsetDateTime) {
                value = offsetDateTime.toLocalDateTime();
            }
            values.add(value);
        }

        String sql = String.format("INSERT INTO \"%s\" (%s) VALUES (%s)",
                tableName,
                columns.substring(0, columns.length() - 1),
                placeholders.substring(0, placeholders.length() - 1));

        jdbcTemplate.update(sql, values.toArray());
    }

    private void insertOrUpdateRow(String tableName, Map<String, Object> row, FileSchema schema) {
        StringBuilder columns = new StringBuilder();
        StringBuilder placeholders = new StringBuilder();
        List<Object> values = new ArrayList<>();

        for (Map.Entry<String, Object> entry : row.entrySet()) {
            columns.append("\"").append(entry.getKey()).append("\",");
            placeholders.append("?,");

            Object value = entry.getValue();
            
            if (value instanceof OffsetDateTime offsetDateTime) {
                value = offsetDateTime.toLocalDateTime();
            }
            values.add(value);
        }

        String columnList = columns.substring(0, columns.length() - 1);
        String placeholderList = placeholders.substring(0, placeholders.length() - 1);

        List<String> primaryKeys = new ArrayList<>();
        List<String> updateSet = new ArrayList<>();

        for (Map.Entry<String, ColumnSchema> entry : schema.getColumns().entrySet()) {
            if (entry.getValue().isPrimaryKey()) {
                primaryKeys.add("\"" + entry.getKey() + "\"");
            } else {
                updateSet.add("\"" + entry.getKey() + "\" = EXCLUDED.\"" + entry.getKey() + "\"");
            }
        }

        String sql = String.format("INSERT INTO \"%s\" (%s) VALUES (%s)", tableName, columnList, placeholderList);

        if (!primaryKeys.isEmpty()) {
            sql += " ON CONFLICT (" + String.join(", ", primaryKeys) + ")";
            if (!updateSet.isEmpty()) {
                sql += " DO UPDATE SET " + String.join(", ", updateSet);
            } else {
                sql += " DO NOTHING";
            }
        }

        jdbcTemplate.update(sql, values.toArray());
    }
}
