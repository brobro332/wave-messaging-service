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

/**
 * STOMP 웹소켓 기반 실시간 채팅 메시지 송수신 및 검색을 처리하는 컨트롤러 클래스입니다.
 * 
 * <p>주요 역할 및 아키텍처 흐름:</p>
 * <ul>
 *   <li><b>실시간 메시징 (STOMP + Redis Pub/Sub)</b>: 클라이언트가 STOMP 웹소켓 프로토콜을 통해 {@code /pub/chat/message}로 메시지를 전송하면, 이를 받아 Redis의 Pub/Sub 채널(Topic)로 발행(Publish)합니다. 이렇게 발행된 메시지는 별도의 Subscriber 서버(인스턴스들)를 통해 각 클라이언트({@code /sub/chat/room/{roomId}})로 브로드캐스팅(Broadcast)되어 다중 서버 환경에서도 안정적인 실시간 채팅이 가능합니다.</li>
 *   <li><b>대화 내역 조회 (RDBMS)</b>: 특정 채팅방 입장 시 가장 최근의 과거 메시지 목록(최대 50개)을 관계형 데이터베이스에서 역순으로 조회하여 제공합니다.</li>
 *   <li><b>메시지 풀텍스트 검색 (Elasticsearch)</b>: 채팅 메시지의 방대한 데이터 중 특정 키워드를 고속으로 검색하기 위해 Elasticsearch를 활용합니다. 형태소 분석(N-Gram 등)된 인덱스를 바탕으로 해당 채팅방 내의 대화를 빠르고 정확하게 찾아냅니다.</li>
 * </ul>
 * 
 * @author 개발팀
 * @see xyz.messaging.wave.service.RedisPublisher
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ChatController {

    private final RedisPublisher redisPublisher;
    private final ChatMessageRepository chatMessageRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    /**
     * 클라이언트로부터 STOMP 웹소켓을 통해 수신된 채팅 메시지를 처리합니다. (엔드포인트: /pub/chat/message)
     * 
     * <p>처리 흐름:</p>
     * <ol>
     *   <li>클라이언트가 보낸 메시지의 타입(ENTER, LEAVE 등)에 따라 자동으로 시스템 안내 메시지 텍스트를 구성합니다.</li>
     *   <li>메시지 발송 시각이 지정되지 않은 경우 현재 서버 시각(HH:mm 포맷)을 설정합니다.</li>
     *   <li>완성된 메시지 객체(DTO)를 Redis Publisher를 통해 지정된 채널로 발행(Publish)하여, 다른 서버 노드 및 구독 중인 클라이언트들에게 전파합니다.</li>
     * </ol>
     *
     * @param message 클라이언트로부터 전달받은 채팅 메시지 객체 (타입, 발신자, 채팅방 ID, 내용 등 포함)
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
     * 특정 채팅방의 최근 과거 대화 내역(최대 50건)을 조회하는 REST API입니다.
     * 
     * <p>채팅방 최초 입장 시 클라이언트가 이전 대화 문맥을 파악할 수 있도록, RDBMS(메인 DB)에서 가장 최근에 저장된 메시지들을 내림차순(최신순)으로 조회하여 반환합니다.</p>
     *
     * @param roomId 조회할 채팅방의 고유 ID
     * @return 상태 코드 200(OK)과 함께 해당 채팅방의 최근 메시지 목록 반환
     */
    @GetMapping("/api/chat/rooms/{roomId}/messages")
    public ResponseEntity<List<ChatMessage>> getRecentMessages(@PathVariable String roomId) {
        List<ChatMessage> messages = chatMessageRepository.findTop50ByRoomIdOrderByIdDesc(roomId);
        return ResponseEntity.ok(messages);
    }

    /**
     * Elasticsearch를 기반으로 특정 채팅방 내 대화 기록에 대해 고속 풀텍스트(Full-Text) 검색을 수행하는 REST API입니다.
     * 
     * <p>검색 처리 흐름:</p>
     * <ul>
     *   <li>채팅방 ID(roomId)로 검색 범위를 한정합니다.</li>
     *   <li>클라이언트가 전달한 키워드(keyword)를 통해 원본 메시지 필드({@code message})의 정확한 일치 여부와, 형태소 분석이 적용된 N-Gram 필드({@code message.ngram})의 부분 일치 여부를 함께 OR 조건으로 검색합니다.</li>
     *   <li>이를 통해 단순 텍스트 매칭보다 훨씬 유연하고 높은 검색 정확도를 보장합니다.</li>
     * </ul>
     *
     * @param roomId 검색을 수행할 채팅방의 고유 ID
     * @param keyword 검색어 키워드
     * @return 상태 코드 200(OK)과 함께 검색 조건에 부합하는 채팅 메시지(Document) 목록 반환
     */
    @GetMapping("/api/chat/rooms/{roomId}/search")
    public ResponseEntity<List<ChatMessageDocument>> searchChatMessages(
            @PathVariable String roomId,
            @RequestParam String keyword
    ) {
        log.info("Searching chat messages in room [{}] for keyword: {}", roomId, keyword);
        
        Criteria criteria = new Criteria("roomId").is(roomId)
                .and(new Criteria("message").is(keyword).or(new Criteria("message.ngram").is(keyword)));

        CriteriaQuery query = new CriteriaQuery(criteria);
        SearchHits<ChatMessageDocument> searchHits = elasticsearchOperations.search(query, ChatMessageDocument.class);

        List<ChatMessageDocument> results = searchHits.stream()
                .map(SearchHit::getContent)
                .toList();

        return ResponseEntity.ok(results);
    }
}
