package com.example.restapi.constants;

/**
 * API Constants for versioning and common paths
 */
public final class ApiConstants {
    
    /**
     * Current API version
     */
    public static final String API_VERSION = "v1";
    
    /**
     * Base API path with version
     */
    public static final String API_BASE_PATH = "/api/" + API_VERSION;
    
    /**
     * Health check API path
     */
    public static final String HEALTH_API_PATH = API_BASE_PATH + "/health";
    
    /**
     * Echo API path
     */
    public static final String ECHO_API_PATH = API_BASE_PATH + "/echo";
    
    /**
     * SQS API path
     */
    public static final String SQS_API_PATH = API_BASE_PATH + "/sqs";
    
    /**
     * SNS API path
     */
    public static final String SNS_API_PATH = API_BASE_PATH + "/sns";
    
    /**
     * Kafka API path
     */
    public static final String KAFKA_API_PATH = API_BASE_PATH + "/kafka";
    
    // Private constructor to prevent instantiation
    private ApiConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
} 