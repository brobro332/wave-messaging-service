package xyz.messaging.wave.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import xyz.messaging.wave.domain.ChatMessage;
import xyz.messaging.wave.domain.ChatMessageDocument;

@Slf4j
@Service
public class ChatMessageKafkaConsumer {

    private final ElasticsearchClient elasticsearchClient;
    private final ObjectMapper objectMapper;

    public ChatMessageKafkaConsumer(ElasticsearchClient elasticsearchClient) {
        this.elasticsearchClient = elasticsearchClient;
        this.objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    @KafkaListener(topics = "chat-messages", groupId = "wave-chat-es-group")
    public void consumeChatMessage(String messageJson) {
        try {
            log.info("Received raw chat message from Kafka for indexing: {}", messageJson);

            ChatMessage chatMsg = objectMapper.readValue(messageJson, ChatMessage.class);
            ChatMessageDocument doc = ChatMessageDocument.from(chatMsg);

            IndexRequest<ChatMessageDocument> request = IndexRequest.of(i -> i
                    .index("chat_messages")
                    .id(doc.getId())
                    .document(doc)
            );
            
            elasticsearchClient.index(request);
            log.info("Successfully indexed chat message to ES: messageId={}, roomId={}", doc.getId(), doc.getRoomId());
        } catch (Exception e) {
            log.error("Failed to process chat message event and index to Elasticsearch", e);
        }
    }
}
