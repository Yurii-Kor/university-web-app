package ua.foxminded.university.service.dto.request.course;

import jakarta.validation.constraints.*;
import static ua.foxminded.university.service.dto.request.ValidationConstants.*;

public record CourseCreateDto(

        @NotBlank
        @Size(min = COURSE_CODE_MIN, max = COURSE_CODE_MAX)
        @Pattern(
             regexp = COURSE_CODE_REGEX,
             message = COURSE_CODE_MESSAGE
        )
        String code,

        @NotBlank
        @Size(max = COURSE_NAME_MAX)
        @Pattern(
             regexp = TEXT_NOT_BLANK_WHEN_PRESENT_REGEX,
             message = TEXT_NOT_BLANK_WHEN_PRESENT_MESSAGE
        )
        String name,

        @Size(max = DESCRIPTION_MAX)
        String description,

        @NotNull
        Long teacherId
) {}
