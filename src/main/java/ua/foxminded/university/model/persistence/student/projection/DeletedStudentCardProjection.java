package ua.foxminded.university.model.persistence.student.projection;

import java.time.Instant;

public interface DeletedStudentCardProjection {
    Long getId();
    String getEmail();
    String getFirstName();
    String getLastName();
    boolean isEnabled();
    Instant getCreatedAt();
    Instant getDeletedAt();
    Integer getEnrollmentYear();
    String getGroupName();
}