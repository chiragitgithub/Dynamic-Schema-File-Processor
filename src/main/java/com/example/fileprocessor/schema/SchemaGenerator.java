package com.example.fileprocessor.schema;

import com.example.fileprocessor.model.FileSchema;
import org.springframework.web.multipart.MultipartFile;

public interface SchemaGenerator {
    FileSchema generateSchema(MultipartFile file, String fileType, String tableName) throws Exception;
}
