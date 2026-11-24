package ua.foxminded.university.web.lesson.page;

import java.time.LocalDate;
import java.util.List;

public record WeeklySchedulePageModel(
        String pageTitle,
        String pageSubtitle,
        LocalDate weekStart,
        LocalDate weekEnd,
        List<ScheduleDayModel> days,
        boolean showTeacherInfo,
        boolean showGroupInfo,
        boolean allowManage
) {}
