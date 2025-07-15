package com.example.fileprocessor.model;

public class ValidationError {

    private int rowNumber;         // CSV row number (1-based)
    private String columnName;     // DB column name
    private String message;        // Error message

    public ValidationError() {}

    public ValidationError(int rowNumber, String columnName, String message) {
        this.rowNumber = rowNumber;
        this.columnName = columnName;
        this.message = message;
    }

    public int getRowNumber() {
        return rowNumber;
    }

    public void setRowNumber(int rowNumber) {
        this.rowNumber = rowNumber;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "Row " + rowNumber + ", Column '" + columnName + "': " + message;
    }
}
