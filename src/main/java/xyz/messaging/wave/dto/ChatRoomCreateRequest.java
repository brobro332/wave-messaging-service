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
public class ChatRoomCreateRequest {
    private String roomId;       // 생성할 방 ID (옵션, 없을 시 자동 생성)
    private String roomName;     // 방 이름 (예: 성수동 닻 모임톡)
    private String roomType;     // 방 타입 (LOCAL, ANCHOR, DIRECT)
    private String targetId;     // 메인 target ID (닻 ID 또는 지하철역 이름)
    private String userId;       // 개설자 유저 ID / 닉네임
    private Long workspaceId;    // 소속 워크스페이스 ID (옵션)
}
