package xyz.messaging.wave.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import xyz.messaging.wave.domain.ChatMessage;
import xyz.messaging.wave.domain.ChatMessageDocument;
import xyz.messaging.wave.repository.ChatMessageElasticsearchRepository;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageKafkaConsumer {

    private final ChatMessageElasticsearchRepository chatMessageElasticsearchRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "chat-messages", groupId = "wave-chat-es-group")
    public void consumeChatMessage(String messageJson) {
        try {
            log.info("Received raw chat message from Kafka for indexing: {}", messageJson);

            ChatMessage chatMsg = objectMapper.readValue(messageJson, ChatMessage.class);
            ChatMessageDocument doc = ChatMessageDocument.from(chatMsg);

            chatMessageElasticsearchRepository.save(doc);
            log.info("Successfully indexed chat message to ES: messageId={}, roomId={}", doc.getId(), doc.getRoomId());
        } catch (Exception e) {
            log.error("Failed to process chat message event and index to Elasticsearch", e);
        }
    }
}
