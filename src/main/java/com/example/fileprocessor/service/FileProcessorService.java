package com.example.fileprocessor.service;

import com.example.fileprocessor.model.ValidationError;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileProcessorService {
    List<ValidationError> processFile(MultipartFile file, String fileType, String mode)   throws Exception;
}
