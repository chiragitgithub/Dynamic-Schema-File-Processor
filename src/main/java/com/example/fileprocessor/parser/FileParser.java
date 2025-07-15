package com.example.fileprocessor.parser;

import com.example.fileprocessor.model.FileSchema;
import com.example.fileprocessor.model.ValidationError;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface FileParser {
    List<Map<String, Object>> parse(MultipartFile file, FileSchema schema, List<ValidationError> errors) throws Exception;
}
