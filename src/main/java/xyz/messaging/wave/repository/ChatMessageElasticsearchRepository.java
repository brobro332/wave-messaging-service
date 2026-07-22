package xyz.messaging.wave.repository;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import xyz.messaging.wave.domain.ChatMessageDocument;

public interface ChatMessageElasticsearchRepository extends ElasticsearchRepository<ChatMessageDocument, String> {
}
