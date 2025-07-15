package com.example.fileprocessor.model;

import java.util.Map;

public class FileSchema {

    private String fileType;
    private String tableName;
    private String mode; // "TABLE" or "DYNAMIC"
    private Map<String, ColumnSchema> columns;

    // Getters and Setters

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public Map<String, ColumnSchema> getColumns() {
        return columns;
    }

    public void setColumns(Map<String, ColumnSchema> columns) {
        this.columns = columns;
    }
}
