package xyz.datt.wave.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import xyz.datt.wave.domain.ChatRoom;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, String> {
    Optional<ChatRoom> findByTargetId(String targetId);
}
