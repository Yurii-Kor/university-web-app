package ua.foxminded.university.service.util.projectionmapper;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import ua.foxminded.university.model.domain.enums.AcademicRank;
import ua.foxminded.university.model.persistence.teacher.projection.DeletedTeacherCardProjection;
import ua.foxminded.university.service.teacher.dto.DeletedTeacherCardView;

@Component
@RequiredArgsConstructor
public class DeletedTeacherCardMapper implements ProjectionMapper<DeletedTeacherCardProjection, DeletedTeacherCardView> {

    private final DateTimeMapper dateTimeMapper;

    @Override
    public DeletedTeacherCardView toView(DeletedTeacherCardProjection p) {
        return new DeletedTeacherCardView(
                p.getId(),
                p.getEmail(),
                p.getFirstName(),
                p.getLastName(),
                p.isEnabled(),
                dateTimeMapper.toUtcOffsetDateTime(p.getCreatedAt()),
                dateTimeMapper.toUtcOffsetDateTime(p.getDeletedAt()),
                AcademicRank.valueOf(p.getAcademicRank()),
                p.getOffice()
        );
    }
}
