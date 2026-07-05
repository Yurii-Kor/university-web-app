package ua.foxminded.university.service.dto.request.teacher;

import jakarta.validation.constraints.*;
import ua.foxminded.university.model.domain.enums.AcademicRank;

import static ua.foxminded.university.service.dto.request.ValidationConstants.*;

public record TeacherSelfUpdateDto(

        @NotNull()
        Long id,

        AcademicRank academicRank,

        @Size(max = ROOM_MAX)
        @Pattern(
            regexp = ROOM_CODE_REGEX,
            message = OFFICE_MESSAGE
        )
        String office
) {}
