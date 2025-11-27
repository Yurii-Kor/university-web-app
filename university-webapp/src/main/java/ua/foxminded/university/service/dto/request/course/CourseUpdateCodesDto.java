package ua.foxminded.university.service.dto.request.course;

import jakarta.validation.constraints.*;
import static ua.foxminded.university.service.dto.request.ValidationConstants.*;

public record CourseUpdateCodesDto(

        @NotNull
        Long id,
        
        @NotBlank
        @Size(min = COURSE_CODE_MIN, max = COURSE_CODE_MAX)
        @Pattern(
            regexp = COURSE_CODE_REGEX,
            message = COURSE_CODE_MESSAGE
        )
        String code
) {}
