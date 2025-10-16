//package com.example.fileprocessor.parser;
//
//import com.example.fileprocessor.model.FileSchema;
//import com.example.fileprocessor.model.ValidationError;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.util.List;
//import java.util.Map;
//
//public interface FileParser {
//    List<Map<String, Object>> parse(MultipartFile file, FileSchema schema, List<ValidationError> errors) throws Exception;
//}

package com.example.fileprocessor.parser;

import com.example.fileprocessor.model.FileSchema;
import com.example.fileprocessor.model.ValidationError;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * Interface for parsing uploaded files (CSV or similar) into structured data.
 */
public interface FileParser {

    /**
     * Parses an uploaded file into a list of maps, where each map represents a row of data.
     * Supports dynamic delimiter handling.
     *
     * @param file      The uploaded file to parse
     * @param schema    The schema definition for the file
     * @param errors    List to collect validation errors during parsing
     * @param delimiter The CSV delimiter character (e.g., ',', '\t', ';', '|')
     * @return List of parsed rows as maps (column name -> value)
     * @throws Exception If file reading or parsing fails
     */
    List<Map<String, Object>> parse(
            MultipartFile file,
            FileSchema schema,
            List<ValidationError> errors,
            char delimiter
    ) throws Exception;
}
