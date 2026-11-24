package ua.foxminded.university.web.lesson.export;

import java.util.List;

import ua.foxminded.university.web.lesson.page.WeeklySchedulePageModel;

public record ScheduleExportData(
        String ownerType,
        String ownerName,
        List<String> ownerDetails,
        WeeklySchedulePageModel schedule
) {}
