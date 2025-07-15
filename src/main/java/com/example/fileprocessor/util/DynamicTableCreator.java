package com.example.fileprocessor.util;

import com.example.fileprocessor.model.FileSchema;
import com.example.fileprocessor.model.ColumnSchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class DynamicTableCreator {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public void createDynamicTableIfNotExists(String tableName, FileSchema schema) {
        StringBuilder createSQL = new StringBuilder("CREATE TABLE IF NOT EXISTS " + sanitizeIdentifier(tableName) + " (");

        List<String> primaryKeys = new ArrayList<>();

        for (var entry : schema.getColumns().entrySet()) {
            String columnName = sanitizeIdentifier(entry.getKey());
            ColumnSchema column = entry.getValue();

            createSQL.append(columnName)
                    .append(" ")
                    .append(getSQLType(column.getType()))
                    .append(", ");

            if (column.isPrimaryKey()) {
                primaryKeys.add(columnName);
            }
        }

        if (!primaryKeys.isEmpty()) {
            createSQL.append("PRIMARY KEY (")
                    .append(String.join(", ", primaryKeys))
                    .append("), ");
        }

        // Remove trailing comma and space
        createSQL.setLength(createSQL.length() - 2);
        createSQL.append(");");

        jdbcTemplate.execute(createSQL.toString());
    }

    private String getSQLType(String jsonType) {
        return switch (jsonType.toLowerCase()) {
            case "string" -> "VARCHAR(255)";
            case "int", "integer" -> "INTEGER";
            case "double" -> "DOUBLE PRECISION";
            case "date" -> "DATE";
            case "timestamp" -> "TIMESTAMP";
            case "boolean" -> "BOOLEAN";
            default -> "TEXT";
        };
    }

    private String sanitizeIdentifier(String identifier) {
        return "\"" + identifier.toLowerCase().replaceAll("[^a-z0-9_]", "_") + "\"";
    }
}
