package xyz.datt.wave.controller;

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
import xyz.datt.wave.domain.ChatMessage;
import xyz.datt.wave.dto.ChatMessageDto;
import xyz.datt.wave.repository.ChatMessageRepository;
import xyz.datt.wave.service.RedisPublisher;

@Slf4j
@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ChatController {

    private final RedisPublisher redisPublisher;
    private final ChatMessageRepository chatMessageRepository;

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
}
