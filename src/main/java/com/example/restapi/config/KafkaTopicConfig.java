package com.example.restapi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "kafka")
public class KafkaTopicConfig {

    private List<TopicConfig> topics;

    public List<TopicConfig> getTopics() {
        return topics;
    }

    public void setTopics(List<TopicConfig> topics) {
        this.topics = topics;
    }

    public static class TopicConfig {
        private String name;
        private String consumerGroup;
        private String correlatedTopic;
        private String keyOfInterest;
        private String correlatedKeyOfInterest;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getConsumerGroup() {
            return consumerGroup;
        }

        public void setConsumerGroup(String consumerGroup) {
            this.consumerGroup = consumerGroup;
        }

        public String getCorrelatedTopic() {
            return correlatedTopic;
        }

        public void setCorrelatedTopic(String correlatedTopic) {
            this.correlatedTopic = correlatedTopic;
        }

        public String getKeyOfInterest() {
            return keyOfInterest;
        }

        public void setKeyOfInterest(String keyOfInterest) {
            this.keyOfInterest = keyOfInterest;
        }

        public String getCorrelatedKeyOfInterest() {
            return correlatedKeyOfInterest;
        }

        public void setCorrelatedKeyOfInterest(String correlatedKeyOfInterest) {
            this.correlatedKeyOfInterest = correlatedKeyOfInterest;
        }

        @Override
        public String toString() {
            return "TopicConfig{" +
                    "name='" + name + '\'' +
                    ", consumerGroup='" + consumerGroup + '\'' +
                    ", correlatedTopic='" + correlatedTopic + '\'' +
                    ", keyOfInterest='" + keyOfInterest + '\'' +
                    ", correlatedKeyOfInterest='" + correlatedKeyOfInterest + '\'' +
                    '}';
        }
    }
} 