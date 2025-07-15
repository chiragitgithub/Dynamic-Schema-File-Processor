package com.example.fileprocessor.schema;

import com.example.fileprocessor.model.FileSchema;

public interface SchemaReader {
    FileSchema readSchema(String fileType) throws Exception;
}