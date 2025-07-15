package com.example.fileprocessor.model;

public class ColumnSchema {

    private String csvColumn;
    private String type;
    private String transform;
    private boolean primaryKey;

    // Getters and Setters

    public String getCsvColumn() {
        return csvColumn;
    }

    public void setCsvColumn(String csvColumn) {
        this.csvColumn = csvColumn;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTransform() {
        return transform;
    }

    public void setTransform(String transform) {
        this.transform = transform;
    }

    public boolean isPrimaryKey() {
        return primaryKey;
    }

    public void setPrimaryKey(boolean primaryKey) {
        this.primaryKey = primaryKey;
    }
}
