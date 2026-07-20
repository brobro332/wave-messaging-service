package xyz.messaging.wave.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageDto {

    public enum MessageType {
        ENTER, TALK, LEAVE
    }

    private MessageType type;    // 메시지 타입 (ENTER, TALK, LEAVE)
    private String roomId;       // 채팅방 ID (targetId: anchorId, stationName 등)
    private String sender;       // 발신자 닉네임
    private String message;      // 메시지 본문
    private String sentAt;       // 전송 시간 (ISO 문자열 또는 HH:mm)
}
