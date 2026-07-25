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

/**
 * 워크스페이스(협업 인프라) 생성 및 사용자 가입, 권한 통제 비즈니스 로직을 캡슐화한 서비스 클래스입니다.
 * 
 * <p>핵심 아키텍처 및 역할:</p>
 * <ul>
 *   <li><b>확장성 있는 공간 분리</b>: 향후 다중 기업(Tenant) 환경이나 복잡한 부서별 구조를 지원하기 위한 논리적 단위인 '워크스페이스'의 라이프사이클을 관장합니다.</li>
 *   <li><b>자동화된 편의성 제공</b>: 빈 공간에 유저를 홀로 남겨두지 않도록, 워크스페이스 개설이나 신규 합류 시점에 시스템이 '기본 소통용 채팅방'을 자동으로 할당하고 입장시켜주는 자동화 훅(Hook)을 포함하고 있습니다.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;

    /**
     * 시스템에 새로운 워크스페이스(공간)를 개설하고 초기 필수 환경(권한, 기본 채팅방)을 세팅하는 트랜잭션 메서드입니다.
     * 
     * <p>상세 수행 단계:</p>
     * <ol>
     *   <li>중복될 확률이 극히 낮은 식별 문자열(UUID 조합, 예: WS-X9Y8Z7)을 생성하여 해당 워크스페이스 전용 초대 코드(Invite Code)로 발급합니다.</li>
     *   <li>워크스페이스 메타 데이터를 RDBMS에 영속화(Save)합니다.</li>
     *   <li>공간을 생성한 주체(요청 유저)를 즉시 최고 관리자({@code role="OWNER"}) 권한을 부여하여 워크스페이스 멤버로 등록합니다.</li>
     *   <li>이 공간 내 인원들이 공통으로 사용할 수 있는 '기본 그룹 채팅방'을 하나 백그라운드에서 자동 개설합니다.</li>
     *   <li>방금 권한을 받은 개설자를 앞서 자동 개설된 기본 채팅방의 멤버로도 자동 입장(Join) 처리하여 초기 셋업을 완결합니다.</li>
     * </ol>
     *
     * @param request 개설 희망 이름, 요청 유저 식별자 등이 포함된 요청 데이터 객체
     * @return 생성된 워크스페이스 식별 키, 명칭, 발급된 초대 코드가 담긴 응답 DTO
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
     * 지정된 사용자가 소속된 모든 워크스페이스 메타 정보를 반환합니다.
     * 
     * <p>매핑 테이블({@code WorkspaceMember})에서 해당 유저의 데이터를 모두 찾은 후, 추출된 워크스페이스 ID 목록을 이용해 실제 워크스페이스 엔티티들을 일괄 조회(IN 쿼리 등)하여 DTO로 변환합니다.</p>
     *
     * @param userId 워크스페이스 리스트를 확인할 유저 식별자
     * @return 해당 유저가 가입된 전체 워크스페이스 목록
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
     * 발급된 '초대 코드(Invite Code)'를 매개체로 하여 기존 워크스페이스에 새로운 인원을 합류시키는 트랜잭션 메서드입니다.
     * 
     * <p>상세 수행 단계:</p>
     * <ul>
     *   <li>클라이언트가 입력한 초대 코드를 대문자로 변환하여 DB 인덱스를 타고 해당 워크스페이스가 실존하는지 검증합니다.</li>
     *   <li>이미 가입된 사용자인지(Idempotency 검증) 체크하여 중복 가입 에러를 방어합니다.</li>
     *   <li>일반 사용자({@code role="MEMBER"}) 권한으로 워크스페이스 멤버십 레코드를 삽입합니다.</li>
     *   <li><b>(중요 편의 기능)</b> 해당 워크스페이스 내에 이미 개설된 방들 중 0번째(통상적으로 기본 채팅방) 방을 찾아내어, 이제 막 가입한 신규 멤버를 그 채팅방에도 즉각적으로 강제 조인(Join) 시킵니다. 이를 통해 가입 직후 즉시 채팅에 참여 가능하도록 유도합니다.</li>
     * </ul>
     *
     * @param inviteCode 공유받은 워크스페이스 초대 코드 문자열
     * @param userId 가입을 승인받을 유저 식별자
     * @return 가입 완료된 워크스페이스의 정보를 담은 응답 DTO
     * @throws IllegalArgumentException 유효하지 않은 코드를 입력한 경우
     * @throws IllegalStateException 이미 가입된 공간에 재가입 시도 시
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
     * 특정 워크스페이스에 속해 있는 구성원 전체의 멤버십(권한, 가입일 등) 정보를 제공합니다.
     * 
     * @param workspaceId 조회 대상 워크스페이스의 내부 식별 ID (PK)
     * @return 소속된 멤버들의 엔티티 리스트
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
