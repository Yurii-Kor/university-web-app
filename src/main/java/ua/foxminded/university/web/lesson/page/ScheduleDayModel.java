package ua.foxminded.university.web.lesson.page;

import java.time.LocalDate;
import java.util.List;

import ua.foxminded.university.model.persistence.lesson.projection.LessonScheduleItemView;

public record ScheduleDayModel(
        LocalDate date,
        boolean today,
        List<LessonScheduleItemView> lessons
) {}
