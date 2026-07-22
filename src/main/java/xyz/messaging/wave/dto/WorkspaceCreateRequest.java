package xyz.messaging.wave.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceCreateRequest {
    private String name;
    private String userId; // 개설 유저 ID
}
