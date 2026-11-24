package ua.foxminded.university.web.lesson.export;

import ua.foxminded.university.web.lesson.page.SchedulePageMode;
import ua.foxminded.university.web.lesson.page.WeeklySchedulePageModel;

public interface ScheduleExportStrategy {

    SchedulePageMode mode();

    ScheduleExportData build(
            long userId,
            WeeklySchedulePageModel schedule);
}