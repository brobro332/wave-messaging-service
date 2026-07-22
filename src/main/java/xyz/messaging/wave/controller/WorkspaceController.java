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

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    /**
     * 신규 워크스페이스 개설 API
     */
    @PostMapping("/api/workspaces")
    public ResponseEntity<WorkspaceResponse> createWorkspace(@RequestBody WorkspaceCreateRequest request) {
        WorkspaceResponse response = workspaceService.createWorkspace(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 유저별 가입된 워크스페이스 목록 조회 API
     */
    @GetMapping("/api/workspaces")
    public ResponseEntity<List<WorkspaceResponse>> getWorkspacesByUser(@RequestParam String userId) {
        List<WorkspaceResponse> responses = workspaceService.getWorkspacesByUser(userId);
        return ResponseEntity.ok(responses);
    }

    /**
     * 초대코드로 워크스페이스 가입 API
     */
    @PostMapping("/api/workspaces/join")
    public ResponseEntity<WorkspaceResponse> joinWorkspace(
            @RequestParam String inviteCode,
            @RequestParam String userId) {
        WorkspaceResponse response = workspaceService.joinWorkspace(inviteCode, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * 특정 워크스페이스의 멤버 목록 조회 API
     */
    @GetMapping("/api/workspaces/{workspaceId}/members")
    public ResponseEntity<List<WorkspaceMember>> getWorkspaceMembers(@PathVariable Long workspaceId) {
        List<WorkspaceMember> members = workspaceService.getWorkspaceMembers(workspaceId);
        return ResponseEntity.ok(members);
    }
}
