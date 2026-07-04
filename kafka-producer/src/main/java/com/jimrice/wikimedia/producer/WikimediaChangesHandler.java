package com.jimrice.wikimedia.producer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class WikimediaChangesHandler implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(WikimediaChangesHandler.class);
    private static final String STREAM_URI = "/v2/stream/recentchange";

    private final WikimediaChangesProducer producer;
    private final WebClient webClient;

    WikimediaChangesHandler(WikimediaChangesProducer producer, WebClient.Builder webClientBuilder) {
        this.producer = producer;
        this.webClient = webClientBuilder
                .baseUrl("https://stream.wikimedia.org")
                .build();
    }

    @Override
    public void run(String... args) {
        log.info("Starting Wikimedia SSE stream -> Kafka pipeline");

        webClient.get()
                .uri(STREAM_URI)
                .retrieve()
                .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {})
                .filter(event -> event.data() != null && !event.data().isBlank())
                .doOnNext(event -> producer.sendMessage(event.data()))
                .doOnError(err -> log.error("Stream error: {}", err.getMessage()))
                .blockLast();
    }
}
