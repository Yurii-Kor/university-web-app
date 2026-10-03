package ua.foxminded.university.web.lesson.export.strategy;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import ua.foxminded.university.service.teacher.TeacherService;
import ua.foxminded.university.web.lesson.export.ScheduleExportData;
import ua.foxminded.university.web.lesson.export.ScheduleExportStrategy;
import ua.foxminded.university.web.lesson.page.SchedulePageMode;
import ua.foxminded.university.web.lesson.page.WeeklySchedulePageModel;

@Component
@RequiredArgsConstructor
public class TeacherScheduleExportStrategy
        implements ScheduleExportStrategy {

    private final TeacherService teacherService;

    @Override
    public SchedulePageMode mode() {
        return SchedulePageMode.TEACHER;
    }

    @Override
    public ScheduleExportData build(
            long userId,
            WeeklySchedulePageModel schedule) {

        var profile =
                teacherService.getTeacherProfileView(
                        userId);

        var ownerName =
                "%s %s".formatted(
                        profile.firstName(),
                        profile.lastName());

        var details =
                List.of(
                        "Academic rank: "
                                + profile.academicRank(),
                        "Office: "
                                + Objects.toString(
                                        profile.office(),
                                        "—"));

        return new ScheduleExportData(
                "Teacher",
                ownerName,
                details,
                schedule);
    }
}