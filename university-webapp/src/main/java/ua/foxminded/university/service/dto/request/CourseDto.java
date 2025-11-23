package ua.foxminded.university.service.dto.request;

import jakarta.validation.constraints.*;
import ua.foxminded.university.service.util.validation.groups.OnCreate;
import ua.foxminded.university.service.util.validation.groups.OnUpdateCodes;
import ua.foxminded.university.service.util.validation.groups.OnUpdateSelf;

public record CourseDto(
        @NotNull(groups = { OnUpdateSelf.class, OnUpdateCodes.class })
        Long id,

        @NotBlank(groups = { OnCreate.class, OnUpdateCodes.class })
        @Size(min = 4, max = 32, groups = { OnCreate.class, OnUpdateCodes.class })
        @Pattern(
            regexp = "^(?:[A-Z]{2,5}-[A-Z]{2,9}-\\d{3}|[A-Z]{2,5}-\\d{3})$",
            message = "Code must look like 'CSE-ALG-101' or 'SEC-303'",
            groups = { OnCreate.class, OnUpdateCodes.class }
        )
        String code,

        @NotBlank(groups = { OnCreate.class })
        @Size(max = 255, groups = { OnCreate.class, OnUpdateSelf.class })
        @Pattern(
            regexp = "^\\s*$|\\S[\\s\\S]*",
            message = "Name must not be blank when provided",
            groups = { OnCreate.class, OnUpdateSelf.class }
        )
        String name,

        @Size(max = 10_000, groups = { OnCreate.class, OnUpdateSelf.class })
        String description,

        @NotNull(groups = { OnCreate.class })
        Long teacherId
) {}
