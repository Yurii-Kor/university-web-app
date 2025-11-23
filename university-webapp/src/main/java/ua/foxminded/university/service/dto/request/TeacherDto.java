package ua.foxminded.university.service.dto.request;

import jakarta.validation.constraints.*;
import ua.foxminded.university.model.domain.enums.AcademicRank;
import ua.foxminded.university.service.util.validation.groups.OnCreate;
import ua.foxminded.university.service.util.validation.groups.OnUpdateSelf;

public record TeacherDto(
        @NotNull(groups = { OnUpdateSelf.class })
        Long id,

        @NotBlank(groups = { OnCreate.class })
        @Email(groups = { OnCreate.class })
        @Size(max = 255, groups = { OnCreate.class })
        String email,

        @NotBlank(groups = { OnCreate.class })
        @Size(min = 8, max = 100, groups = { OnCreate.class })
        @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[\\p{Punct}_])(?=\\S+$).{8,100}$",
            message = "Password must be 8–100 chars, include upper/lowercase, digit, special, and contain no spaces",
            groups = { OnCreate.class }
        )
        String password,

        @NotBlank(groups = { OnCreate.class })
        @Size(max = 64, groups = { OnCreate.class })
        @Pattern(regexp = "^[A-Za-z]+(?:[-'][A-Za-z]+)*$", groups = { OnCreate.class })
        String firstName,

        @NotBlank(groups = { OnCreate.class })
        @Size(max = 64, groups = { OnCreate.class })
        @Pattern(regexp = "^[A-Za-z]+(?:[-'][A-Za-z]+)*$", groups = { OnCreate.class })
        String lastName,

        @NotNull(groups = { OnCreate.class })
        AcademicRank academicRank,

        @NotBlank(groups = { OnCreate.class })
        @Pattern(
            regexp = "^[A-Z]-\\d{3}$",
            message = "Office must look like 'B-105' (Letter-3digits)",
            groups = { OnCreate.class, OnUpdateSelf.class }
        )
        String office
) {}
