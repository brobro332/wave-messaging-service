package xyz.datt.wave.controller;

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
import xyz.datt.wave.dto.ChatRoomCreateRequest;
import xyz.datt.wave.dto.ChatRoomResponse;
import xyz.datt.wave.service.ChatRoomService;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ChatRoomController {

    private final ChatRoomService chatRoomService;

    /**
     * 신규 채팅방 생성 API
     */
    @PostMapping("/api/chat/rooms")
    public ResponseEntity<ChatRoomResponse> createRoom(@RequestBody ChatRoomCreateRequest request) {
        ChatRoomResponse response = chatRoomService.createRoom(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 특정 톡방 참가 API
     */
    @PostMapping("/api/chat/rooms/{roomId}/join")
    public ResponseEntity<Void> joinRoom(@PathVariable String roomId, @RequestParam String userId) {
        chatRoomService.joinRoom(roomId, userId);
        return ResponseEntity.ok().build();
    }

    /**
     * 유저별 참여 톡방 목록 및 안 읽은 메시지 수(Unread Count) 조회 API
     */
    @GetMapping("/api/chat/rooms")
    public ResponseEntity<List<ChatRoomResponse>> getRoomsByUser(@RequestParam String userId) {
        List<ChatRoomResponse> responses = chatRoomService.getRoomsByUser(userId);
        return ResponseEntity.ok(responses);
    }

    /**
     * 특정 톡방 읽음 처리 API (lastReadAt 갱신하여 unreadCount -> 0)
     */
    @PostMapping("/api/chat/rooms/{roomId}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable String roomId, @RequestParam String userId) {
        chatRoomService.markAsRead(roomId, userId);
        return ResponseEntity.ok().build();
    }
}
