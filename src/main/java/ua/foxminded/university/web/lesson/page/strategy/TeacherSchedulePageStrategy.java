package ua.foxminded.university.web.lesson.page.strategy;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import ua.foxminded.university.model.persistence.lesson.projection.LessonScheduleItemView;
import ua.foxminded.university.service.lesson.LessonService;
import ua.foxminded.university.web.lesson.page.SchedulePageMode;
import ua.foxminded.university.web.lesson.page.SchedulePageStrategy;

@Component
@RequiredArgsConstructor
public class TeacherSchedulePageStrategy
        implements SchedulePageStrategy {

    private final LessonService lessonService;

    @Override
    public SchedulePageMode mode() {

        return SchedulePageMode.TEACHER;
    }

    @Override
    public List<LessonScheduleItemView> loadLessons(
            long userId,
            OffsetDateTime from,
            OffsetDateTime to) {

        return lessonService.listTeacherScheduleItems(
                userId,
                from,
                to);
    }
}