package xyz.messaging.wave.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import xyz.messaging.wave.domain.ChatRoom;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, String> {
    Optional<ChatRoom> findByTargetId(String targetId);
    List<ChatRoom> findAllByWorkspaceId(Long workspaceId);
}
