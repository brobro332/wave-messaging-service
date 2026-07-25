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

/**
 * 채팅방 라이프사이클 관리 및 메시지 카운팅 비즈니스 로직을 처리하는 서비스 클래스입니다.
 * 
 * <p>주요 비즈니스 로직:</p>
 * <ul>
 *   <li><b>채팅방 개설 및 트랜잭션 보장</b>: 단일 채팅방 데이터 생성뿐만 아니라, 개설자를 최초의 멤버로 자동 등록하는 과정을 하나의 트랜잭션 묶음으로 처리하여 데이터 무결성을 보장합니다.</li>
 *   <li><b>안 읽은 메시지 정밀 연산</b>: 단순한 전체 메시지 개수가 아닌, 각 사용자가 채팅방 단위로 갖고 있는 마지막 읽음 시점({@code lastReadAt})을 기준으로 그 이후에 발행(Insert)된 메시지의 수를 RDBMS COUNT 쿼리로 계산해 정확한 Unread Count를 도출합니다.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatMessageRepository chatMessageRepository;

    /**
     * 새로운 채팅방을 생성하고, 요청한 유저(개설자)를 해당 채팅방의 멤버로 즉시 등록하는 비즈니스 로직입니다.
     * 
     * <p>실행 흐름:</p>
     * <ol>
     *   <li>클라이언트가 별도의 Room ID를 지정하지 않았다면 UUID를 이용해 8자리의 고유 ID(ex: room_1a2b3c4d)를 발급합니다.</li>
     *   <li>ChatRoom 엔티티를 빌드하여 DB에 우선 저장(INSERT)합니다.</li>
     *   <li>요청 데이터에 userId가 포함되어 있다면, 내부 메서드인 {@code joinRoom()}을 호출하여 해당 유저의 ChatRoomMember 엔티티를 생성 및 저장합니다.</li>
     *   <li>최초 생성 시점이므로 안 읽은 메시지는 0개로 설정하여 응답 DTO를 구성합니다.</li>
     * </ol>
     *
     * @param request 생성할 채팅방 이름, 타입, 소속 워크스페이스 등의 데이터 전달 객체
     * @return 영속화된 채팅방 정보와 초기화된 Unread Count(0)가 포함된 DTO
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
     * 유저를 특정 채팅방의 멤버로 합류시키고, 향후 새 메시지 알림 기준이 될 읽음 시각을 초기화합니다.
     * 
     * <p>실행 흐름:</p>
     * <ul>
     *   <li>동일 유저가 동일 방에 중복으로 조인하는 것을 방지하기 위해 DB에서 이미 참여 중인지 확인({@code findByRoomIdAndUserId})합니다.</li>
     *   <li>조회 결과가 없다면(신규 합류라면), 새로운 {@code ChatRoomMember} 객체를 생성하고 가입 시각(joinedAt)과 최근 읽음 시각(lastReadAt)을 현재 시각으로 설정하여 저장합니다.</li>
     * </ul>
     *
     * @param roomId 참가 대상 채팅방 고유 식별자
     * @param userId 방에 참가할 사용자 식별자
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
     * 특정 워크스페이스(협업 공간)에 종속된 모든 채팅방 목록을 불러오고, 현재 유저 관점에서의 '안 읽은 알림 수'를 매핑하여 반환합니다.
     * 
     * <p>비즈니스 특징: 성능 최적화보다는 정확성에 초점을 맞추어 설계되었습니다. 조회된 모든 채팅방에 대해 순회하며, 해당 유저가 멤버로 있는지 검사한 뒤 읽음 기준선(lastReadAt)을 바탕으로 안 읽은 카운트를 산출합니다.</p>
     *
     * @param workspaceId 채팅방을 조회할 기준 워크스페이스 ID
     * @param userId 안 읽은 메시지를 계산할 기준 유저 ID
     * @return 워크스페이스 내 방 목록과 각각의 안 읽은 메시지 수가 결합된 DTO 리스트
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
     * 지정된 유저가 현재 참여하고 있는 모든 채팅방 목록을 조회하고, 각 방의 '안 읽은 메시지 수(Unread Count)'를 정밀하게 계산하여 응답합니다.
     * 
     * <p>실행 흐름:</p>
     * <ol>
     *   <li>유저 ID를 조건으로 {@code ChatRoomMember} 매핑 테이블을 조회하여 속한 방의 ID 목록을 추출합니다.</li>
     *   <li>각 방 ID에 해당하는 {@code ChatRoom} 엔티티를 조회합니다.</li>
     *   <li>유저의 해당 방 마지막 읽음 시각(lastReadAt)이 존재하면, 메시지 테이블({@code ChatMessage})에서 그 시각 <b>이후</b>에 생성된 메시지 개수를 COUNT하여 Unread 카운트를 구합니다.</li>
     *   <li>읽음 시각이 전혀 없다면 해당 방 전체 메시지 수를 안 읽은 카운트로 간주합니다.</li>
     * </ol>
     *
     * @param userId 조회를 요청한 유저 ID
     * @return 유저가 속한 각 방의 기본 정보 및 계산 완료된 안 읽은 메시지 수 포함 DTO 리스트
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
     * 유저가 특정 채팅방에 진입하여 최신 메시지까지 모두 확인(읽음)했음을 서버에 기록하는 로직입니다.
     * 
     * <p>해당 방에 대한 유저의 구독/참여 정보({@code ChatRoomMember})를 조회한 뒤, {@code lastReadAt} 필드의 값을 현재 서버의 타임스탬프로 업데이트합니다. 이 업데이트가 완료되면 향후 목록 조회 시 기준선이 앞당겨져 해당 방의 안 읽은 메시지 수가 자연스럽게 0으로 계산됩니다.</p>
     *
     * @param roomId 유저가 읽음 처리하려는 대상 채팅방 ID
     * @param userId 액션을 수행한 유저 ID
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
