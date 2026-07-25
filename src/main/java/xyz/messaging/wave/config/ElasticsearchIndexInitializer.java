package xyz.messaging.wave.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.DeleteIndexRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ElasticsearchIndexInitializer {

    private final ElasticsearchClient elasticsearchClient;

    @EventListener(ApplicationReadyEvent.class)
    public void initializeIndices() {
        try {
            log.info("Deleting existing 'chat_messages' index for applying new mapping...");
            try {
                elasticsearchClient.indices().delete(DeleteIndexRequest.of(d -> d.index("chat_messages")));
            } catch (Exception ignore) {
                // Ignore if it doesn't exist
            }

            log.info("Creating Elasticsearch 'chat_messages' index...");
            elasticsearchClient.indices().create(CreateIndexRequest.of(c -> c.index("chat_messages")));
            log.info("Successfully created new 'chat_messages' index.");

        } catch (Exception e) {
            log.error("Failed to initialize Elasticsearch 'chat_messages' index", e);
        }
    }
}
