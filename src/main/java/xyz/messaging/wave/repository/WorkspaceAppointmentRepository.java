package xyz.messaging.wave.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.messaging.wave.domain.WorkspaceAppointment;

import java.util.List;

public interface WorkspaceAppointmentRepository extends JpaRepository<WorkspaceAppointment, Long> {
    List<WorkspaceAppointment> findByWorkspaceIdOrderByAppointmentTimeAsc(Long workspaceId);
}
