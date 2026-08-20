package ua.foxminded.university.service.student.dto;

import static ua.foxminded.university.service.util.validation.ValidationConstants.ENROLLMENT_YEAR_MAX;
import static ua.foxminded.university.service.util.validation.ValidationConstants.ENROLLMENT_YEAR_MIN;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record StudentEnrollmentYearUpdateDto(

        @NotNull
        @Min(ENROLLMENT_YEAR_MIN)
        @Max(ENROLLMENT_YEAR_MAX)
        Integer enrollmentYear
) {
}