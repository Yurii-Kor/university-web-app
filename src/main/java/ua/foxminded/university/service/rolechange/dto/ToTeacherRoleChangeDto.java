package ua.foxminded.university.service.rolechange.dto;

import static ua.foxminded.university.service.util.validation.ValidationConstants.OFFICE_MESSAGE;
import static ua.foxminded.university.service.util.validation.ValidationConstants.ROOM_CODE_REGEX;
import static ua.foxminded.university.service.util.validation.ValidationConstants.ROOM_MAX;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import ua.foxminded.university.model.domain.enums.AcademicRank;
import ua.foxminded.university.service.rolechange.target.strategy.data.ToTeacherRoleProfileData;

public record ToTeacherRoleChangeDto(

        @NotNull
        AcademicRank academicRank,

        @NotBlank
        @Size(max = ROOM_MAX)
        @Pattern(regexp = ROOM_CODE_REGEX, message = OFFICE_MESSAGE)
        String office

) implements ToTeacherRoleProfileData {}