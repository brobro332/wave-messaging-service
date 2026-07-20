package xyz.datt.wave.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoomResponse {
    private String roomId;
    private String roomName;
    private String roomType;
    private String targetId;
    private String lastMessage;
    private LocalDateTime lastMessageAt;
    private long unreadCount;     // 🔥 안 읽은 메시지 개수 (Unread Count)
    private LocalDateTime createdAt;
}
