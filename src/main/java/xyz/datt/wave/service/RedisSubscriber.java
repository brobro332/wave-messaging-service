package xyz.datt.wave.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import xyz.datt.wave.domain.ChatMessage;
import xyz.datt.wave.dto.ChatMessageDto;
import xyz.datt.wave.repository.ChatMessageRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisSubscriber {

    private final ObjectMapper objectMapper;
    private final SimpMessageSendingOperations messagingTemplate;
    private final ChatMessageRepository chatMessageRepository;

    public void sendMessage(String publishMessage) {
        try {
            // Redis에서 발행된 JSON 문자열 역직렬화
            ChatMessageDto chatMessage = objectMapper.readValue(publishMessage, ChatMessageDto.class);

            // STOMP 구독자들에게 웹소켓 메시지 실시간 브로드캐스팅 (/sub/chat/room/{roomId})
            messagingTemplate.convertAndSend("/sub/chat/room/" + chatMessage.getRoomId(), chatMessage);

            // DB 비동기/영구 저장
            ChatMessage entity = ChatMessage.builder()
                    .roomId(chatMessage.getRoomId())
                    .sender(chatMessage.getSender())
                    .message(chatMessage.getMessage())
                    .messageType(chatMessage.getType().name())
                    .createdAt(LocalDateTime.now())
                    .build();
            chatMessageRepository.save(entity);

            log.info("Successfully broadcasted and saved chat message for room: {}", chatMessage.getRoomId());
        } catch (Exception e) {
            log.error("Exception in RedisSubscriber handling message", e);
        }
    }
}
