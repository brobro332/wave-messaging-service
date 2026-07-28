package xyz.messaging.wave.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class WorkspaceAppointmentRequest {
    private String title;
    private String description;
    private LocalDateTime appointmentTime;
    private String location;
}
