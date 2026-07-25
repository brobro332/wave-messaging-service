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
import xyz.messaging.wave.domain.WorkspaceMember;
import xyz.messaging.wave.dto.WorkspaceCreateRequest;
import xyz.messaging.wave.dto.WorkspaceResponse;
import xyz.messaging.wave.service.WorkspaceService;

/**
 * 워크스페이스(협업 공간)의 개설, 유저 가입, 멤버 및 목록 조회를 담당하는 REST API 컨트롤러 클래스입니다.
 * 
 * <p>주요 비즈니스 역할:</p>
 * <ul>
 *   <li><b>협업 공간 분리</b>: 슬랙(Slack)이나 디스코드(Discord)와 유사하게 프로젝트나 조직 단위로 소통 공간(Workspace)을 논리적으로 분리하고 관리합니다.</li>
 *   <li><b>초대 기반 가입</b>: 워크스페이스 생성 시 발급되는 고유 초대 코드(Invite Code)를 통해 다른 사용자들이 손쉽게 해당 워크스페이스에 참여할 수 있도록 지원합니다.</li>
 *   <li><b>기본 소통 채널 제공</b>: 워크스페이스 개설 혹은 가입 즉시 자동으로 기본 채팅방(General 채널 성격)에 소속되도록 유도하여 초기 진입 장벽을 낮춥니다.</li>
 * </ul>
 */
@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    /**
     * 새로운 워크스페이스(협업 공간)를 개설하는 API입니다.
     * 
     * <p>요청된 이름으로 워크스페이스를 생성하며 시스템 내부적으로 6자리의 고유 초대 코드를 자동 발급합니다. 또한, 워크스페이스를 개설한 사용자를 최고 관리자(OWNER) 권한으로 자동 등록하며, 이 공간 내의 '기본 채팅방'을 하나 생성하여 개설자를 즉시 입장시킵니다.</p>
     *
     * @param request 개설할 워크스페이스 정보 (워크스페이스 명칭 및 개설자 유저 ID 등)
     * @return 상태 코드 200(OK)과 함께 생성 완료된 워크스페이스의 상세 정보 반환
     */
    @PostMapping("/api/workspaces")
    public ResponseEntity<WorkspaceResponse> createWorkspace(@RequestBody WorkspaceCreateRequest request) {
        WorkspaceResponse response = workspaceService.createWorkspace(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 특정 유저가 가입되어 활동 중인 워크스페이스 목록을 조회하는 API입니다.
     * 
     * <p>유저와 워크스페이스 간의 매핑 정보(WorkspaceMember)를 기반으로 해당 유저가 접근 권한을 가진 협업 공간 목록만 추출하여 반환합니다. 클라이언트의 좌측 글로벌 네비게이션(GNB) 영역에서 워크스페이스 전환 아이콘을 렌더링하는 데 사용됩니다.</p>
     *
     * @param userId 조회 대상 유저의 식별 ID
     * @return 상태 코드 200(OK)과 함께 가입된 워크스페이스 목록 리스트 반환
     */
    @GetMapping("/api/workspaces")
    public ResponseEntity<List<WorkspaceResponse>> getWorkspacesByUser(@RequestParam String userId) {
        List<WorkspaceResponse> responses = workspaceService.getWorkspacesByUser(userId);
        return ResponseEntity.ok(responses);
    }

    /**
     * 공유받은 초대 코드(Invite Code)를 입력하여 기존 워크스페이스에 가입하는 API입니다.
     * 
     * <p>초대 코드의 유효성과 이미 가입된 유저인지 중복 여부를 검증한 뒤 일반 멤버(MEMBER) 권한으로 가입 처리합니다. 아울러 해당 워크스페이스의 메인/기본 채팅방을 찾아 신규 가입 유저를 자동으로 조인(Join)시켜 즉각적인 소통이 가능하도록 구성합니다.</p>
     *
     * @param inviteCode 가입하고자 하는 워크스페이스의 고유 초대 문자열 (예: WS-A1B2C3)
     * @param userId 가입을 요청한 유저의 식별 ID
     * @return 상태 코드 200(OK)과 함께 성공적으로 가입된 워크스페이스 정보 반환
     */
    @PostMapping("/api/workspaces/join")
    public ResponseEntity<WorkspaceResponse> joinWorkspace(
            @RequestParam String inviteCode,
            @RequestParam String userId) {
        WorkspaceResponse response = workspaceService.joinWorkspace(inviteCode, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * 지정한 워크스페이스에 가입된 전체 멤버(사용자) 목록 및 권한 정보를 조회하는 API입니다.
     * 
     * <p>해당 워크스페이스 내에서 멘션(Mention) 기능을 사용하거나, 새로운 그룹 채팅방 개설을 위해 동료 목록을 불러올 때 사용됩니다. 각 멤버의 가입일, 권한(OWNER/MEMBER 등) 데이터가 포함됩니다.</p>
     *
     * @param workspaceId 조회 대상 워크스페이스의 고유 ID (PK)
     * @return 상태 코드 200(OK)과 함께 해당 공간에 소속된 멤버 리스트 반환
     */
    @GetMapping("/api/workspaces/{workspaceId}/members")
    public ResponseEntity<List<WorkspaceMember>> getWorkspaceMembers(@PathVariable Long workspaceId) {
        List<WorkspaceMember> members = workspaceService.getWorkspaceMembers(workspaceId);
        return ResponseEntity.ok(members);
    }
}
