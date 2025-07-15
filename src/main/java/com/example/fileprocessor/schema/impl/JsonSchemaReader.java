package com.example.fileprocessor.schema.impl;

import com.example.fileprocessor.model.FileSchema;
import com.example.fileprocessor.schema.SchemaReader;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Component
public class JsonSchemaReader implements SchemaReader {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public FileSchema readSchema(String fileType) throws Exception {
        String schemaPath = "/schemas/" + fileType + ".json";

        InputStream is = getClass().getResourceAsStream(schemaPath);

        if (is == null) {
            throw new IllegalArgumentException("Schema not found for fileType: " + fileType);
        }

        return objectMapper.readValue(is, FileSchema.class);
    }
}
