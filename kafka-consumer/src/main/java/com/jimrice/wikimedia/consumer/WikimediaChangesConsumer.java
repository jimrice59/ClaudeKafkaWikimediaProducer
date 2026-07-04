package com.jimrice.wikimedia.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class WikimediaChangesConsumer {

    private static final Logger log = LoggerFactory.getLogger(WikimediaChangesConsumer.class);

    @KafkaListener(topics = "wikimedia.recentchange", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(String message) {
        log.info("Received: {}", message);
    }
}
