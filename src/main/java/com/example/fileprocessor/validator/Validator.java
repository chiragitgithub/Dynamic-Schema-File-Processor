package com.example.fileprocessor.validator;

public interface Validator {
    Object validate(String value, String expectedType, String transformation) throws Exception;
}
