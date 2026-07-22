package xyz.messaging.wave.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.messaging.wave.domain.Workspace;
import xyz.messaging.wave.domain.WorkspaceMember;
import xyz.messaging.wave.dto.WorkspaceCreateRequest;
import xyz.messaging.wave.dto.WorkspaceResponse;
import xyz.messaging.wave.repository.WorkspaceMemberRepository;
import xyz.messaging.wave.repository.WorkspaceRepository;
import xyz.messaging.wave.repository.ChatRoomRepository;
import xyz.messaging.wave.repository.ChatRoomMemberRepository;
import xyz.messaging.wave.domain.ChatRoom;
import xyz.messaging.wave.domain.ChatRoomMember;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;

    /**
     * 신규 워크스페이스 개설
     */
    @Transactional
    public WorkspaceResponse createWorkspace(WorkspaceCreateRequest request) {
        // 고유 초대 코드 생성 (UUID 앞 8자리)
        String inviteCode = "WS-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        Workspace workspace = Workspace.builder()
                .name(request.getName())
                .inviteCode(inviteCode)
                .createdAt(LocalDateTime.now())
                .build();

        workspaceRepository.save(workspace);

        // 개설자 가입 처리 (OWNER)
        WorkspaceMember member = WorkspaceMember.builder()
                .workspaceId(workspace.getId())
                .userId(request.getUserId())
                .role("OWNER")
                .joinedAt(LocalDateTime.now())
                .build();

        workspaceMemberRepository.save(member);

        // 워크스페이스 전용 1개 고유 기본 채팅방 자동 생성 및 저장
        String roomId = "ws_room_" + UUID.randomUUID().toString().substring(0, 8);
        ChatRoom chatRoom = ChatRoom.builder()
                .roomId(roomId)
                .roomName(workspace.getName())
                .roomType("LOCAL")
                .targetId("WORKSPACE_" + workspace.getId())
                .workspaceId(workspace.getId())
                .createdAt(LocalDateTime.now())
                .build();
        chatRoomRepository.save(chatRoom);

        // 개설자를 이 채팅방의 멤버로도 자동 조인 등록
        ChatRoomMember chatRoomMember = ChatRoomMember.builder()
                .roomId(roomId)
                .userId(request.getUserId())
                .lastReadAt(LocalDateTime.now())
                .joinedAt(LocalDateTime.now())
                .build();
        chatRoomMemberRepository.save(chatRoomMember);

        return convertToResponse(workspace);
    }

    /**
     * 유저별 가입한 워크스페이스 목록 조회
     */
    public List<WorkspaceResponse> getWorkspacesByUser(String userId) {
        List<WorkspaceMember> members = workspaceMemberRepository.findByUserId(userId);
        List<Long> workspaceIds = members.stream()
                .map(WorkspaceMember::getWorkspaceId)
                .collect(Collectors.toList());

        return workspaceRepository.findAllById(workspaceIds).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 초대 코드로 워크스페이스 가입
     */
    @Transactional
    public WorkspaceResponse joinWorkspace(String inviteCode, String userId) {
        Workspace workspace = workspaceRepository.findByInviteCode(inviteCode.trim().toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 초대 코드입니다."));

        // 중복 가입 체크
        workspaceMemberRepository.findByWorkspaceIdAndUserId(workspace.getId(), userId)
                .ifPresent(m -> {
                    throw new IllegalStateException("이미 가입된 워크스페이스입니다.");
                });

        WorkspaceMember member = WorkspaceMember.builder()
                .workspaceId(workspace.getId())
                .userId(userId)
                .role("MEMBER")
                .joinedAt(LocalDateTime.now())
                .build();

        workspaceMemberRepository.save(member);

        // 해당 워크스페이스에 개설된 고유 기본 채팅방을 찾아서 신규 가입 유저도 자동 조인 처리
        List<ChatRoom> rooms = chatRoomRepository.findAllByWorkspaceId(workspace.getId());
        if (!rooms.isEmpty()) {
            ChatRoom targetRoom = rooms.get(0);
            
            // 이미 방 멤버인지 중복 가입 체크 후 추가
            if (!chatRoomMemberRepository.findByRoomIdAndUserId(targetRoom.getRoomId(), userId).isPresent()) {
                ChatRoomMember chatRoomMember = ChatRoomMember.builder()
                        .roomId(targetRoom.getRoomId())
                        .userId(userId)
                        .lastReadAt(LocalDateTime.now())
                        .joinedAt(LocalDateTime.now())
                        .build();
                chatRoomMemberRepository.save(chatRoomMember);
            }
        }

        return convertToResponse(workspace);
    }

    /**
     * 워크스페이스 가입 유저 목록 조회
     */
    public List<WorkspaceMember> getWorkspaceMembers(Long workspaceId) {
        return workspaceMemberRepository.findByWorkspaceId(workspaceId);
    }

    private WorkspaceResponse convertToResponse(Workspace workspace) {
        return WorkspaceResponse.builder()
                .id(workspace.getId())
                .name(workspace.getName())
                .inviteCode(workspace.getInviteCode())
                .createdAt(workspace.getCreatedAt())
                .build();
    }
}
