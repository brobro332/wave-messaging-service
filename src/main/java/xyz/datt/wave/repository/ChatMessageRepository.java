package xyz.datt.wave.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import xyz.datt.wave.domain.ChatMessage;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findTop50ByRoomIdOrderByIdDesc(String roomId);
    long countByRoomIdAndCreatedAtAfter(String roomId, LocalDateTime after);
    long countByRoomId(String roomId);
}
