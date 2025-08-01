package com.example.restapi.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Service
public class SqsService {

    private static final Logger logger = LoggerFactory.getLogger(SqsService.class);

    private final SqsClient sqsClient;

    @Value("${aws.sqs.queue-url}")
    private String queueUrl;

    public SqsService(SqsClient sqsClient) {
        this.sqsClient = sqsClient;
    }

    /**
     * Send a message to SQS queue
     */
    public SendMessageResponse sendMessage(String message) {
        try {
            SendMessageRequest sendMessageRequest = SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(message)
                    .build();

            SendMessageResponse response = sqsClient.sendMessage(sendMessageRequest);
            logger.info("Message sent to SQS queue. Message ID: {}", response.messageId());
            return response;
        } catch (Exception e) {
            logger.error("Error sending message to SQS queue", e);
            throw new RuntimeException("Failed to send message to SQS queue", e);
        }
    }

    /**
     * Receive messages from SQS queue
     */
    public List<Message> receiveMessages(int maxMessages) {
        try {
            ReceiveMessageRequest receiveMessageRequest = ReceiveMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .maxNumberOfMessages(maxMessages)
                    .waitTimeSeconds(20) // Long polling
                    .build();

            ReceiveMessageResponse response = sqsClient.receiveMessage(receiveMessageRequest);
            logger.info("Received {} messages from SQS queue", response.messages().size());
            return response.messages();
        } catch (Exception e) {
            logger.error("Error receiving messages from SQS queue", e);
            throw new RuntimeException("Failed to receive messages from SQS queue", e);
        }
    }

    /**
     * Delete a message from SQS queue
     */
    public void deleteMessage(String receiptHandle) {
        try {
            DeleteMessageRequest deleteMessageRequest = DeleteMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .receiptHandle(receiptHandle)
                    .build();

            sqsClient.deleteMessage(deleteMessageRequest);
            logger.info("Message deleted from SQS queue. Receipt handle: {}", receiptHandle);
        } catch (Exception e) {
            logger.error("Error deleting message from SQS queue", e);
            throw new RuntimeException("Failed to delete message from SQS queue", e);
        }
    }

    /**
     * Get queue attributes
     */
    public GetQueueAttributesResponse getQueueAttributes() {
        try {
            GetQueueAttributesRequest request = GetQueueAttributesRequest.builder()
                    .queueUrl(queueUrl)
                    .attributeNames(QueueAttributeName.ALL)
                    .build();

            return sqsClient.getQueueAttributes(request);
        } catch (Exception e) {
            logger.error("Error getting queue attributes", e);
            throw new RuntimeException("Failed to get queue attributes", e);
        }
    }
} 