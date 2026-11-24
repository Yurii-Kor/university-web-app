package ua.foxminded.university.web.lesson.page.strategy;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import ua.foxminded.university.model.persistence.lesson.projection.LessonScheduleItemView;
import ua.foxminded.university.service.lesson.LessonService;
import ua.foxminded.university.service.studygroup.StudyGroupService;
import ua.foxminded.university.web.lesson.page.SchedulePageMode;
import ua.foxminded.university.web.lesson.page.SchedulePageStrategy;

@Component
@RequiredArgsConstructor
public class StudentSchedulePageStrategy
        implements SchedulePageStrategy {

    private final LessonService lessonService;
    private final StudyGroupService groupService;

    @Override
    public SchedulePageMode mode() {

        return SchedulePageMode.STUDENT;
    }

    @Override
    public List<LessonScheduleItemView> loadLessons(
            long userId,
            OffsetDateTime from,
            OffsetDateTime to) {

        var group =
                groupService.getGroupHeaderForStudent(
                        userId);

        return lessonService.listGroupScheduleItems(
                group.id(),
                from,
                to);
    }
}