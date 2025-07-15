package com.example.fileprocessor.controller;

import com.example.fileprocessor.model.ValidationError;
import com.example.fileprocessor.service.FileProcessorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/files")
public class FileUploadController {

    private final FileProcessorService fileProcessorService;

    public FileUploadController(FileProcessorService fileProcessorService) {
        this.fileProcessorService = fileProcessorService;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("fileType") String fileType,
            @RequestParam(value = "mode", defaultValue = "TABLE") String mode
    ) {
        System.out.println("Received mode: " + mode);
        try {
            List<ValidationError> errors = fileProcessorService.processFile(file, fileType, mode.toUpperCase());

            if (errors.isEmpty()) {
                return ResponseEntity.ok("File processed successfully and data inserted.");
            } else {
                return ResponseEntity.badRequest().body(errors);
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid input: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error processing file: " + e.getMessage());
        }
    }
}
