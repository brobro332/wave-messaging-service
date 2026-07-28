package xyz.messaging.wave.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import xyz.messaging.wave.domain.WorkspaceAppointment;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class WorkspaceAppointmentResponse {
    private Long id;
    private Long workspaceId;
    private String title;
    private String description;
    private LocalDateTime appointmentTime;
    private String location;
    private Long creatorId;
    private LocalDateTime createdAt;

    public static WorkspaceAppointmentResponse from(WorkspaceAppointment entity) {
        return WorkspaceAppointmentResponse.builder()
                .id(entity.getId())
                .workspaceId(entity.getWorkspaceId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .appointmentTime(entity.getAppointmentTime())
                .location(entity.getLocation())
                .creatorId(entity.getCreatorId())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
