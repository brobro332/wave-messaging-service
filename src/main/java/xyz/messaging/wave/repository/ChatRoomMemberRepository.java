package xyz.messaging.wave.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import xyz.messaging.wave.domain.ChatRoomMember;

public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, Long> {
    List<ChatRoomMember> findAllByUserId(String userId);
    Optional<ChatRoomMember> findByRoomIdAndUserId(String roomId, String userId);
}
