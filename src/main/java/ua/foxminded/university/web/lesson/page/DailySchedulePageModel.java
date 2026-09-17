package ua.foxminded.university.web.lesson.page;

import java.time.LocalDate;
import java.util.List;

import ua.foxminded.university.model.persistence.lesson.projection.LessonScheduleItemView;

public record DailySchedulePageModel(
        String pageTitle,
        String pageSubtitle,
        LocalDate date,
        boolean today,
        List<LessonScheduleItemView> lessons,
        boolean showTeacherInfo,
        boolean showGroupInfo,
        boolean allowManage
) {}
