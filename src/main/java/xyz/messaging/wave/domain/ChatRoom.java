package xyz.messaging.wave.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "wave_chat_rooms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoom {

    @Id
    @Column(nullable = false, unique = true)
    private String roomId;          // 방 ID (예: room_anchor_50, room_gangnam)

    @Column(nullable = false)
    private String roomName;        // 방 이름 (예: 성수동 닻 모임톡, 강남역 실시간톡)

    @Column(nullable = false)
    private String roomType;        // 방 타입 (LOCAL, ANCHOR, DIRECT)

    private String targetId;        // 연동 메인 ID (닻 ID, 장소 ID 등)

    @Column(columnDefinition = "TEXT")
    private String lastMessage;     // 마지막 대화 내용

    private LocalDateTime lastMessageAt; // 마지막 대화 시각

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
