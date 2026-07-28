package xyz.messaging.wave.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import xyz.messaging.wave.dto.ChatRoomCreateRequest;
import xyz.messaging.wave.dto.ChatRoomResponse;
import xyz.messaging.wave.service.ChatRoomService;

/**
 * 채팅방(Chat Room)의 생성, 참가, 목록 조회 및 읽음 처리 등을 담당하는 REST API 컨트롤러 클래스입니다.
 * 
 * <p>주요 비즈니스 역할:</p>
 * <ul>
 *   <li><b>채팅방 라이프사이클 관리</b>: 새로운 채팅방(1:1, 그룹, 워크스페이스용 등)을 개설하고, 유저가 특정 방에 참가(Join)하는 로직을 제공합니다.</li>
 *   <li><b>안 읽은 메시지(Unread Count) 산출</b>: 각 채팅방의 마지막 읽음 시각(lastReadAt)과 현재까지 발행된 전체 메시지를 비교하여 유저별/채팅방별 안 읽은 메시지 수를 정확하게 제공합니다.</li>
 *   <li><b>목록 제공</b>: 특정 유저가 속한 전체 채팅방 리스트 또는 특정 워크스페이스 내에 속한 채팅방 리스트를 필터링하여 응답합니다.</li>
 * </ul>
 */
@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ChatRoomController {

    private final ChatRoomService chatRoomService;

    /**
     * 새로운 채팅방을 생성하는 API입니다.
     * 
     * <p>클라이언트로부터 전달받은 채팅방 유형(개인, 그룹, 워크스페이스 등)과 대상 정보를 바탕으로 고유한 방을 개설하며, 개설을 요청한 유저는 자동으로 해당 채팅방의 첫 멤버로 등록됩니다.</p>
     *
     * @param request 생성할 채팅방의 정보 (이름, 타입, 대상 ID, 소속 워크스페이스 ID 등)
     * @return 상태 코드 200(OK)과 함께 생성된 채팅방의 상세 정보 응답
     */
    @PostMapping("/api/chat/rooms")
    public ResponseEntity<ChatRoomResponse> createRoom(@RequestBody ChatRoomCreateRequest request) {
        ChatRoomResponse response = chatRoomService.createRoom(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 특정 채팅방에 유저를 멤버로 참가시키는 API입니다.
     * 
     * <p>해당 유저를 채팅방의 참여자(ChatRoomMember) 관계 테이블에 추가하며, 참가한 시점부터 새로운 메시지를 정상적으로 수신하고 안 읽은 카운트를 계산할 수 있도록 마지막 읽음 시각(lastReadAt)을 초기화합니다.</p>
     *
     * @param roomId 참가하려는 대상 채팅방의 고유 ID
     * @param userId 참가할 유저의 식별 ID
     * @return 상태 코드 200(OK) 반환 (데이터 본문 없음)
     */
    @PostMapping("/api/chat/rooms/{roomId}/join")
    public ResponseEntity<Void> joinRoom(@PathVariable String roomId, @RequestParam String userId) {
        chatRoomService.joinRoom(roomId, userId);
        return ResponseEntity.ok().build();
    }

    /**
     * 특정 워크스페이스에 종속된 채팅방 목록과 해당 유저의 안 읽은 메시지 수를 조회하는 API입니다.
     * 
     * <p>워크스페이스 단위로 분리된 사내/그룹 커뮤니케이션 환경에서, 현재 유저가 확인하지 못한 알림 건수를 함께 산출하여 좌측 LNB(Local Navigation Bar) 등의 UI에 표현할 수 있도록 데이터를 제공합니다.</p>
     *
     * @param workspaceId 조회할 워크스페이스의 고유 ID (PK)
     * @param userId 현재 조회 요청을 보낸 유저의 식별 ID
     * @return 상태 코드 200(OK)과 함께 워크스페이스 소속 채팅방 목록 및 각각의 안 읽은 카운트 반환
     */
    @GetMapping("/api/chat/workspaces/{workspaceId}/rooms")
    public ResponseEntity<List<ChatRoomResponse>> getRoomsByWorkspace(
            @PathVariable Long workspaceId,
            @RequestParam String userId) {
        List<ChatRoomResponse> responses = chatRoomService.getRoomsByWorkspace(workspaceId, userId);
        return ResponseEntity.ok(responses);
    }

    /**
     * 현재 유저가 소속(참여)된 전체 채팅방 목록 및 각 방의 안 읽은 메시지 수(Unread Count)를 조회하는 API입니다.
     * 
     * <p>메인 채팅 탭(목록 뷰)에서 사용되며, 유저의 참여 정보를 바탕으로 관계된 채팅방만 필터링한 후, 각 방별로 마지막으로 읽은 시점(lastReadAt) 이후에 발생한 신규 메시지 개수를 실시간으로 계산하여 응답합니다.</p>
     *
     * @param userId 조회 요청 유저의 식별 ID
     * @return 상태 코드 200(OK)과 함께 유저가 참여 중인 채팅방 목록 및 안 읽은 메시지 수 반환
     */
    @GetMapping("/api/chat/rooms")
    public ResponseEntity<List<ChatRoomResponse>> getRoomsByUser(@RequestParam String userId) {
        List<ChatRoomResponse> responses = chatRoomService.getRoomsByUser(userId);
        return ResponseEntity.ok(responses);
    }

    /**
     * 특정 채팅방의 메시지들을 모두 읽음 처리(마크)하는 API입니다.
     * 
     * <p>클라이언트가 특정 채팅방 화면에 진입하거나 포커스를 활성화했을 때 호출됩니다. 유저-채팅방 관계 엔티티의 마지막 읽음 시각(lastReadAt)을 현재 서버 시각으로 갱신함으로써, 이후 해당 방의 안 읽은 메시지(Unread Count)를 0으로 초기화시키는 역할을 합니다.</p>
     *
     * @param roomId 읽음 처리할 채팅방의 고유 ID
     * @param userId 요청 유저의 식별 ID
     * @return 상태 코드 200(OK) 반환 (데이터 본문 없음)
     */
    @PostMapping("/api/chat/rooms/{roomId}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable String roomId, @RequestParam String userId) {
        chatRoomService.markAsRead(roomId, userId);
        return ResponseEntity.ok().build();
    }
}
