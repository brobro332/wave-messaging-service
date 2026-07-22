package xyz.messaging.wave.controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import xyz.messaging.wave.domain.ChatMessage;
import xyz.messaging.wave.domain.ChatMessageDocument;
import xyz.messaging.wave.dto.ChatMessageDto;
import xyz.messaging.wave.repository.ChatMessageRepository;
import xyz.messaging.wave.service.RedisPublisher;

@Slf4j
@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ChatController {

    private final RedisPublisher redisPublisher;
    private final ChatMessageRepository chatMessageRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    /**
     * STOMP 메시지 발행 처리 (/pub/chat/message)
     */
    @MessageMapping("/chat/message")
    public void message(ChatMessageDto message) {
        if (ChatMessageDto.MessageType.ENTER.equals(message.getType())) {
            message.setMessage(message.getSender() + "님이 톡방에 입장하셨습니다.");
        } else if (ChatMessageDto.MessageType.LEAVE.equals(message.getType())) {
            message.setMessage(message.getSender() + "님이 톡방에서 퇴장하셨습니다.");
        }

        if (message.getSentAt() == null) {
            message.setSentAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        }

        log.info("Received STOMP Message for Room [{}]: {}", message.getRoomId(), message.getMessage());
        // Redis Topic으로 Publish
        redisPublisher.publish(message);
    }

    /**
     * 과거 대화 내역 조회 REST API (최근 50개)
     */
    @GetMapping("/api/chat/rooms/{roomId}/messages")
    public ResponseEntity<List<ChatMessage>> getRecentMessages(@PathVariable String roomId) {
        List<ChatMessage> messages = chatMessageRepository.findTop50ByRoomIdOrderByIdDesc(roomId);
        return ResponseEntity.ok(messages);
    }

    /**
     * Elasticsearch 기반 채팅방 대화 풀텍스트 검색 API
     */
    @GetMapping("/api/chat/rooms/{roomId}/search")
    public ResponseEntity<List<ChatMessageDocument>> searchChatMessages(
            @PathVariable String roomId,
            @RequestParam String keyword
    ) {
        log.info("Searching chat messages in room [{}] for keyword: {}", roomId, keyword);
        
        Criteria criteria = new Criteria("roomId").is(roomId)
                .and(new Criteria("message").contains(keyword));

        CriteriaQuery query = new CriteriaQuery(criteria);
        SearchHits<ChatMessageDocument> searchHits = elasticsearchOperations.search(query, ChatMessageDocument.class);

        List<ChatMessageDocument> results = searchHits.stream()
                .map(SearchHit::getContent)
                .toList();

        return ResponseEntity.ok(results);
    }
}
