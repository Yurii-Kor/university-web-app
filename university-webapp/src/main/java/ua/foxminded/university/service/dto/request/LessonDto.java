package ua.foxminded.university.service.dto.request;

import jakarta.validation.constraints.*;
import ua.foxminded.university.model.domain.enums.LessonType;
import ua.foxminded.university.service.util.validation.groups.OnCreate;
import ua.foxminded.university.service.util.validation.groups.OnUpdateSelf;

import java.time.OffsetDateTime;

public record LessonDto(
        @NotNull(groups = OnUpdateSelf.class)
        @Null(groups = OnCreate.class)
        Long id,

        @NotNull(groups = { OnCreate.class, OnUpdateSelf.class })
        Long teacherId,

        @NotNull(groups = OnCreate.class)
        Long courseId,

        @NotNull(groups = OnCreate.class)
        Long groupId,

        @NotNull(groups = OnCreate.class)
        OffsetDateTime startTime,

        @NotNull(groups = OnCreate.class)
        OffsetDateTime endTime,

        @NotBlank(groups = OnCreate.class)
        @Size(max = 64, groups = { OnCreate.class, OnUpdateSelf.class })
        @Pattern(regexp = "^[A-Z]-\\d{3}$", 
        		 message = "Room must look like 'B-105'",
                 groups = { OnCreate.class, OnUpdateSelf.class }
        )
        String room,

        LessonType lessonType,

        String description
) {}
