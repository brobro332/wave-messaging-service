package xyz.datt.wave.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.datt.wave.domain.ChatMessage;
import xyz.datt.wave.domain.ChatRoom;
import xyz.datt.wave.dto.ChatMessageDto;
import xyz.datt.wave.repository.ChatMessageRepository;
import xyz.datt.wave.repository.ChatRoomRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisSubscriber {

    private final ObjectMapper objectMapper;
    private final SimpMessageSendingOperations messagingTemplate;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;

    @Transactional
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

            // ChatRoom의 lastMessage 및 lastMessageAt 자동 갱신
            ChatRoom room = chatRoomRepository.findById(chatMessage.getRoomId())
                    .orElseGet(() -> chatRoomRepository.save(
                            ChatRoom.builder()
                                    .roomId(chatMessage.getRoomId())
                                    .roomName("채팅방 " + chatMessage.getRoomId())
                                    .roomType("LOCAL")
                                    .createdAt(LocalDateTime.now())
                                    .build()
                    ));

            room.setLastMessage(chatMessage.getMessage());
            room.setLastMessageAt(LocalDateTime.now());
            chatRoomRepository.save(room);

            log.info("Successfully broadcasted and saved chat message for room: {}", chatMessage.getRoomId());
        } catch (Exception e) {
            log.error("Exception in RedisSubscriber handling message", e);
        }
    }
}
