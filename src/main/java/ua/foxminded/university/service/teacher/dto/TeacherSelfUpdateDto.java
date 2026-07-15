package ua.foxminded.university.service.teacher.dto;

import static ua.foxminded.university.service.util.validation.ValidationConstants.*;

import jakarta.validation.constraints.*;
import ua.foxminded.university.model.domain.enums.AcademicRank;

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
