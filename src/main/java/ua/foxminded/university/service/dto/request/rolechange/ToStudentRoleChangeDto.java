package ua.foxminded.university.service.dto.request.rolechange;

import static ua.foxminded.university.service.dto.request.ValidationConstants.ENROLLMENT_YEAR_MAX;
import static ua.foxminded.university.service.dto.request.ValidationConstants.ENROLLMENT_YEAR_MIN;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import ua.foxminded.university.service.rolechange.target.strategy.data.ToStudentRoleProfileData;

public record ToStudentRoleChangeDto(

        @NotNull
        Long groupId,

        @NotNull
        @Min(ENROLLMENT_YEAR_MIN)
        @Max(ENROLLMENT_YEAR_MAX)
        Integer enrollmentYear

) implements ToStudentRoleProfileData {}