package ua.foxminded.university.web.account.form;

import static ua.foxminded.university.service.dto.request.ValidationConstants.ENROLLMENT_YEAR_MAX;
import static ua.foxminded.university.service.dto.request.ValidationConstants.ENROLLMENT_YEAR_MIN;
import static ua.foxminded.university.service.dto.request.ValidationConstants.OFFICE_MESSAGE;
import static ua.foxminded.university.service.dto.request.ValidationConstants.ROOM_CODE_REGEX;
import static ua.foxminded.university.service.dto.request.ValidationConstants.ROOM_MAX;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import ua.foxminded.university.model.domain.enums.AcademicRank;
import ua.foxminded.university.model.domain.enums.UserRole;
import ua.foxminded.university.service.rolechange.target.strategy.data.ToStudentRoleProfileData;
import ua.foxminded.university.service.rolechange.target.strategy.data.ToTeacherRoleProfileData;

public record RoleChangeForm(

        @NotNull
        UserRole targetRole,

        @NotNull(groups = ToTeacherRoleProfileData.class)
        AcademicRank academicRank,

        @NotBlank(groups = ToTeacherRoleProfileData.class)
        @Size(max = ROOM_MAX, groups = ToTeacherRoleProfileData.class)
        @Pattern(
                regexp = ROOM_CODE_REGEX,
                message = OFFICE_MESSAGE,
                groups = ToTeacherRoleProfileData.class
        )
        String office,

        @NotNull(groups = ToStudentRoleProfileData.class)
        Long groupId,

        @NotNull(groups = ToStudentRoleProfileData.class)
        @Min(value = ENROLLMENT_YEAR_MIN, groups = ToStudentRoleProfileData.class)
        @Max(value = ENROLLMENT_YEAR_MAX, groups = ToStudentRoleProfileData.class)
        Integer enrollmentYear

) implements ToTeacherRoleProfileData, ToStudentRoleProfileData {
}