package xyz.messaging.wave.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.stereotype.Component;
import xyz.messaging.wave.domain.ChatMessageDocument;

@Slf4j
@Component
@RequiredArgsConstructor
public class ElasticsearchIndexInitializer {

    private final ElasticsearchOperations elasticsearchOperations;

    @EventListener(ApplicationReadyEvent.class)
    public void initializeIndices() {
        try {
            IndexOperations indexOps = elasticsearchOperations.indexOps(ChatMessageDocument.class);
            try {
                log.info("Attempting to create Elasticsearch 'chat_messages' index with Nori mapping...");
                indexOps.create();
                indexOps.putMapping(indexOps.createMapping(ChatMessageDocument.class));
                log.info("Successfully initialized Elasticsearch 'chat_messages' index and mapping.");
            } catch (Exception existEx) {
                log.info("Elasticsearch 'chat_messages' index already exists or creation skipped. Reason: {}", existEx.getMessage());
            }
        } catch (Exception e) {
            log.error("Failed to initialize Elasticsearch 'chat_messages' index mapping", e);
        }
    }
}
