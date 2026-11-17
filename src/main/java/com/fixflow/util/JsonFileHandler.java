package com.fixflow.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.type.CollectionType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility for reading and writing data to formatted JSON files using Jackson.
 * Implements File Handling and Generics for the FixFlow persistence layer.
 */
public class JsonFileHandler {

    private static final Logger LOGGER = Logger.getLogger(JsonFileHandler.class.getName());
    private static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        // Configure mapper for human-readable, indented JSON output
        MAPPER.enable(SerializationFeature.INDENT_OUTPUT);
        // Do not fail if JSON contains computed getter properties
        MAPPER.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private JsonFileHandler() {
        // Utility class
    }

    /**
     * Reads a list of objects of type T from a JSON file.
     * If the file does not exist or is empty, returns an empty ArrayList.
     *
     * @param filePath path to the JSON file
     * @param clazz class of elements in the list
     * @param <T> element type
     * @return List of elements loaded from file, or empty list
     */
    public static <T> List<T> loadList(String filePath, Class<T> clazz) {
        File file = new File(filePath);
        if (!file.exists() || file.length() == 0) {
            return new ArrayList<>();
        }

        try {
            CollectionType listType = MAPPER.getTypeFactory().constructCollectionType(ArrayList.class, clazz);
            List<T> data = MAPPER.readValue(file, listType);
            if (data == null) {
                return new ArrayList<>();
            }
            return data;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error reading JSON from " + filePath + ": " + e.getMessage(), e);
            System.err.println("Warning: Could not read data from " + filePath + ". Returning empty list.");
            return new ArrayList<>();
        }
    }

    /**
     * Writes a list of objects of type T to a JSON file.
     * Automatically creates parent directories if they do not exist.
     *
     * @param filePath path to the target JSON file
     * @param list data list to write
     * @param <T> element type
     */
    public static <T> void saveList(String filePath, List<T> list) {
        File file = new File(filePath);
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            MAPPER.writeValue(file, list);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error saving JSON to " + filePath + ": " + e.getMessage(), e);
            System.err.println("Error: Failed to save data to " + filePath + ": " + e.getMessage());
        }
    }
}
