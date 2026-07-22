package xyz.messaging.wave.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.messaging.wave.domain.ChatRoom;
import xyz.messaging.wave.domain.ChatRoomMember;
import xyz.messaging.wave.dto.ChatRoomCreateRequest;
import xyz.messaging.wave.dto.ChatRoomResponse;
import xyz.messaging.wave.repository.ChatMessageRepository;
import xyz.messaging.wave.repository.ChatRoomMemberRepository;
import xyz.messaging.wave.repository.ChatRoomRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatMessageRepository chatMessageRepository;

    /**
     * 새로운 채팅방 생성 및 개설자 멤버 자동 등록
     */
    @Transactional
    public ChatRoomResponse createRoom(ChatRoomCreateRequest request) {
        String roomId = request.getRoomId();
        if (roomId == null || roomId.trim().isEmpty()) {
            roomId = "room_" + UUID.randomUUID().toString().substring(0, 8);
        }

        ChatRoom chatRoom = ChatRoom.builder()
                .roomId(roomId)
                .roomName(request.getRoomName() != null ? request.getRoomName() : "신규 톡방")
                .roomType(request.getRoomType() != null ? request.getRoomType() : "LOCAL")
                .targetId(request.getTargetId())
                .workspaceId(request.getWorkspaceId())
                .createdAt(LocalDateTime.now())
                .build();

        ChatRoom savedRoom = chatRoomRepository.save(chatRoom);

        // 개설자 멤버 추가
        if (request.getUserId() != null && !request.getUserId().trim().isEmpty()) {
            joinRoom(savedRoom.getRoomId(), request.getUserId());
        }

        return convertToResponse(savedRoom, 0);
    }

    /**
     * 특정 톡방에 유저 참가 및 lastReadAt 초기화
     */
    @Transactional
    public void joinRoom(String roomId, String userId) {
        chatRoomMemberRepository.findByRoomIdAndUserId(roomId, userId)
                .orElseGet(() -> chatRoomMemberRepository.save(
                        ChatRoomMember.builder()
                                .roomId(roomId)
                                .userId(userId)
                                .lastReadAt(LocalDateTime.now())
                                .joinedAt(LocalDateTime.now())
                                .build()
                ));
    }

    /**
     * 특정 워크스페이스 내 개설된 톡방 목록 및 안 읽은 메시지 수 조회
     */
    @Transactional(readOnly = true)
    public List<ChatRoomResponse> getRoomsByWorkspace(Long workspaceId, String userId) {
        List<ChatRoom> rooms = chatRoomRepository.findAllByWorkspaceId(workspaceId);
        List<ChatRoomResponse> responses = new ArrayList<>();

        for (ChatRoom room : rooms) {
            long unreadCount = 0;
            java.util.Optional<ChatRoomMember> memberOpt = chatRoomMemberRepository.findByRoomIdAndUserId(room.getRoomId(), userId);
            if (memberOpt.isPresent()) {
                ChatRoomMember member = memberOpt.get();
                if (member.getLastReadAt() != null) {
                    unreadCount = chatMessageRepository.countByRoomIdAndCreatedAtAfter(room.getRoomId(), member.getLastReadAt());
                } else {
                    unreadCount = chatMessageRepository.countByRoomId(room.getRoomId());
                }
            }
            responses.add(convertToResponse(room, unreadCount));
        }

        return responses;
    }

    /**
     * 유저별 참여 톡방 목록 및 안 읽은 메시지 수(Unread Count) 정밀 계산 조회
     */
    @Transactional(readOnly = true)
    public List<ChatRoomResponse> getRoomsByUser(String userId) {
        List<ChatRoomMember> memberships = chatRoomMemberRepository.findAllByUserId(userId);
        List<ChatRoomResponse> responses = new ArrayList<>();

        for (ChatRoomMember member : memberships) {
            chatRoomRepository.findById(member.getRoomId()).ifPresent(room -> {
                long unreadCount = 0;
                if (member.getLastReadAt() != null) {
                    // 마지막으로 읽은 시각 이후에 발행된 메시지 개수 계산
                    unreadCount = chatMessageRepository.countByRoomIdAndCreatedAtAfter(room.getRoomId(), member.getLastReadAt());
                } else {
                    unreadCount = chatMessageRepository.countByRoomId(room.getRoomId());
                }

                responses.add(convertToResponse(room, unreadCount));
            });
        }

        return responses;
    }

    /**
     * 특정 톡방 읽음 처리 (lastReadAt 시각을 현재 시각으로 갱신)
     */
    @Transactional
    public void markAsRead(String roomId, String userId) {
        ChatRoomMember member = chatRoomMemberRepository.findByRoomIdAndUserId(roomId, userId)
                .orElseGet(() -> ChatRoomMember.builder()
                        .roomId(roomId)
                        .userId(userId)
                        .joinedAt(LocalDateTime.now())
                        .build());

        member.setLastReadAt(LocalDateTime.now());
        chatRoomMemberRepository.save(member);
        log.info("User [{}] marked room [{}] as read at {}", userId, roomId, member.getLastReadAt());
    }

    private ChatRoomResponse convertToResponse(ChatRoom room, long unreadCount) {
        return ChatRoomResponse.builder()
                .roomId(room.getRoomId())
                .roomName(room.getRoomName())
                .roomType(room.getRoomType())
                .targetId(room.getTargetId())
                .lastMessage(room.getLastMessage())
                .lastMessageAt(room.getLastMessageAt())
                .unreadCount(unreadCount)
                .createdAt(room.getCreatedAt())
                .build();
    }
}
