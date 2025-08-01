package com.example.restapi.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Service
public class SnsService {

    private static final Logger logger = LoggerFactory.getLogger(SnsService.class);

    private final SnsClient snsClient;

    @Value("${aws.sns.topic-arn}")
    private String topicArn;

    public SnsService(SnsClient snsClient) {
        this.snsClient = snsClient;
    }

    /**
     * Publish a message to SNS topic
     */
    public PublishResponse publishMessage(String message) {
        try {
            PublishRequest publishRequest = PublishRequest.builder()
                    .topicArn(topicArn)
                    .message(message)
                    .build();

            PublishResponse response = snsClient.publish(publishRequest);
            logger.info("Message published to SNS topic. Message ID: {}", response.messageId());
            return response;
        } catch (Exception e) {
            logger.error("Error publishing message to SNS topic", e);
            throw new RuntimeException("Failed to publish message to SNS topic", e);
        }
    }

    /**
     * Publish a message with subject to SNS topic
     */
    public PublishResponse publishMessageWithSubject(String subject, String message) {
        try {
            PublishRequest publishRequest = PublishRequest.builder()
                    .topicArn(topicArn)
                    .subject(subject)
                    .message(message)
                    .build();

            PublishResponse response = snsClient.publish(publishRequest);
            logger.info("Message with subject published to SNS topic. Message ID: {}", response.messageId());
            return response;
        } catch (Exception e) {
            logger.error("Error publishing message with subject to SNS topic", e);
            throw new RuntimeException("Failed to publish message with subject to SNS topic", e);
        }
    }

    /**
     * Subscribe an email to SNS topic
     */
    public SubscribeResponse subscribeEmail(String emailAddress) {
        try {
            SubscribeRequest subscribeRequest = SubscribeRequest.builder()
                    .topicArn(topicArn)
                    .protocol("email")
                    .endpoint(emailAddress)
                    .build();

            SubscribeResponse response = snsClient.subscribe(subscribeRequest);
            logger.info("Email subscribed to SNS topic. Subscription ARN: {}", response.subscriptionArn());
            return response;
        } catch (Exception e) {
            logger.error("Error subscribing email to SNS topic", e);
            throw new RuntimeException("Failed to subscribe email to SNS topic", e);
        }
    }

    /**
     * Subscribe an HTTP/HTTPS endpoint to SNS topic
     */
    public SubscribeResponse subscribeHttpEndpoint(String endpointUrl) {
        try {
            SubscribeRequest subscribeRequest = SubscribeRequest.builder()
                    .topicArn(topicArn)
                    .protocol("https")
                    .endpoint(endpointUrl)
                    .build();

            SubscribeResponse response = snsClient.subscribe(subscribeRequest);
            logger.info("HTTP endpoint subscribed to SNS topic. Subscription ARN: {}", response.subscriptionArn());
            return response;
        } catch (Exception e) {
            logger.error("Error subscribing HTTP endpoint to SNS topic", e);
            throw new RuntimeException("Failed to subscribe HTTP endpoint to SNS topic", e);
        }
    }

    /**
     * List all subscriptions for the topic
     */
    public List<Subscription> listSubscriptions() {
        try {
            ListSubscriptionsByTopicRequest request = ListSubscriptionsByTopicRequest.builder()
                    .topicArn(topicArn)
                    .build();

            ListSubscriptionsByTopicResponse response = snsClient.listSubscriptionsByTopic(request);
            logger.info("Retrieved {} subscriptions for SNS topic", response.subscriptions().size());
            return response.subscriptions();
        } catch (Exception e) {
            logger.error("Error listing subscriptions for SNS topic", e);
            throw new RuntimeException("Failed to list subscriptions for SNS topic", e);
        }
    }

    /**
     * Unsubscribe from SNS topic
     */
    public void unsubscribe(String subscriptionArn) {
        try {
            UnsubscribeRequest unsubscribeRequest = UnsubscribeRequest.builder()
                    .subscriptionArn(subscriptionArn)
                    .build();

            snsClient.unsubscribe(unsubscribeRequest);
            logger.info("Unsubscribed from SNS topic. Subscription ARN: {}", subscriptionArn);
        } catch (Exception e) {
            logger.error("Error unsubscribing from SNS topic", e);
            throw new RuntimeException("Failed to unsubscribe from SNS topic", e);
        }
    }
} 