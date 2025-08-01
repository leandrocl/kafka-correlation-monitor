package com.example.restapi.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sns.SnsClient;

@Configuration
public class AwsConfig {

    @Value("${aws.region}")
    private String region;

    @Value("${aws.access-key-id:}")
    private String accessKeyId;

    @Value("${aws.secret-access-key:}")
    private String secretAccessKey;

    @Bean
    public SqsClient sqsClient() {
        if (accessKeyId.isEmpty() || secretAccessKey.isEmpty()) {
            // Use default credential provider chain
            return SqsClient.builder()
                    .region(Region.of(region))
                    .build();
        } else {
            // Use explicit credentials
            return SqsClient.builder()
                    .region(Region.of(region))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
                    .build();
        }
    }

    @Bean
    public SnsClient snsClient() {
        if (accessKeyId.isEmpty() || secretAccessKey.isEmpty()) {
            // Use default credential provider chain
            return SnsClient.builder()
                    .region(Region.of(region))
                    .build();
        } else {
            // Use explicit credentials
            return SnsClient.builder()
                    .region(Region.of(region))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
                    .build();
        }
    }
} 