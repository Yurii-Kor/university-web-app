package ua.foxminded.university.service.util.projectionmapper;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import ua.foxminded.university.model.repository.dto.DeletedStudentCardProjection;
import ua.foxminded.university.service.dto.response.DeletedStudentCardView;

@Component
@RequiredArgsConstructor
public class DeletedStudentCardMapper
        implements ProjectionMapper<DeletedStudentCardProjection, DeletedStudentCardView> {

    private final DateTimeMapper dateTimeMapper;

    @Override
    public DeletedStudentCardView toView(DeletedStudentCardProjection p) {
        return new DeletedStudentCardView(
                p.getId(),
                p.getEmail(),
                p.getFirstName(),
                p.getLastName(),
                p.isEnabled(),
                dateTimeMapper.toUtcOffsetDateTime(p.getCreatedAt()),
                dateTimeMapper.toUtcOffsetDateTime(p.getDeletedAt()),
                p.getEnrollmentYear(),
                p.getGroupName()
        );
    }
}
