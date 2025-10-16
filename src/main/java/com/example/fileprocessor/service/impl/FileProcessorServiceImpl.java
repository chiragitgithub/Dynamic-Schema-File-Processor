
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
            throw new IllegalArgumentException("Invalid mode. Allowed values are: TABLE or DYNAMIC");
        }

        FileSchema schema = schemaReader.readSchema(fileType);
        List<ValidationError> errors = new ArrayList<>();


        // Use comma as default if delimiter not set
        char delimiter = schema.getDelimiter() != null ? schema.getDelimiter() : ',';

        List<Map<String, Object>> rows = fileParser.parse(file, schema, errors, delimiter);

        if ("DYNAMIC".equalsIgnoreCase(mode)) {
            dynamicTableCreator.createDynamicTableIfNotExists(fileType, schema);
        }

        for (int i = 0; i < rows.size(); i++) {
            if (!hasErrorForRow(errors, i + 1)) {
                if ("DYNAMIC".equalsIgnoreCase(mode)) {
                    insertOrUpdateRow(fileType, rows.get(i), schema);
                } else {
                    insertRow(schema.getTableName(), rows.get(i), schema);
                }
            }
        }

        return errors;
    }

    private boolean hasErrorForRow(List<ValidationError> errors, int rowNum) {
        return errors.stream().anyMatch(e -> e.getRowNumber() == rowNum);
    }

    private void insertRow(String tableName, Map<String, Object> row, FileSchema schema) {
        handleDynamicColumnTypes(tableName, row, schema);

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
        handleDynamicColumnTypes(tableName, row, schema);

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

    private void handleDynamicColumnTypes(String tableName, Map<String, Object> row, FileSchema schema) {
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            String colName = entry.getKey();
            ColumnSchema colSchema = schema.getColumns().get(colName);
            Object value = entry.getValue();

            if (colSchema != null && "string".equalsIgnoreCase(colSchema.getType()) && value != null) {
                String strVal = value.toString();
                if (strVal.length() > 255) {
                    checkAndAlterColumnIfNeeded(tableName, colName);
                }
            }
        }
    }

    /**
     * Dynamically alters column type to TEXT if it's currently VARCHAR
     */
    private void checkAndAlterColumnIfNeeded(String tableName, String columnName) {
        String checkSql = "SELECT data_type FROM information_schema.columns " +
                "WHERE table_name = ? AND column_name = ?";
        String dataType = jdbcTemplate.queryForObject(checkSql, new Object[]{tableName, columnName}, String.class);

        if ("character varying".equalsIgnoreCase(dataType)) {
            String alterSql = String.format("ALTER TABLE \"%s\" ALTER COLUMN \"%s\" TYPE TEXT", tableName, columnName);
            jdbcTemplate.execute(alterSql);
            System.out.println("Altered column " + columnName + " to TEXT dynamically");
        }
    }
}
