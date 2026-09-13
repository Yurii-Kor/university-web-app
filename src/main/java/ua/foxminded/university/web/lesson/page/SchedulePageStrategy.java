package ua.foxminded.university.web.lesson.page;

import java.time.OffsetDateTime;
import java.util.List;

import ua.foxminded.university.model.persistence.lesson.projection.LessonScheduleItemView;

public interface SchedulePageStrategy {

    SchedulePageMode mode();

    List<LessonScheduleItemView> loadLessons(
            long userId,
            OffsetDateTime from,
            OffsetDateTime to);
}