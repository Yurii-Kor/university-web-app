package ua.foxminded.university.web.lesson.export.strategy;

import java.util.List;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import ua.foxminded.university.service.student.StudentService;
import ua.foxminded.university.web.lesson.export.ScheduleExportData;
import ua.foxminded.university.web.lesson.export.ScheduleExportStrategy;
import ua.foxminded.university.web.lesson.page.SchedulePageMode;
import ua.foxminded.university.web.lesson.page.WeeklySchedulePageModel;

@Component
@RequiredArgsConstructor
public class StudentScheduleExportStrategy
        implements ScheduleExportStrategy {

    private final StudentService studentService;

    @Override
    public SchedulePageMode mode() {
        return SchedulePageMode.STUDENT;
    }

    @Override
    public ScheduleExportData build(
            long userId,
            WeeklySchedulePageModel schedule) {

        var profile =
                studentService.getStudentProfileView(
                        userId);

        var ownerName =
                "%s %s".formatted(
                        profile.firstName(),
                        profile.lastName());

        var details =
                List.of(
                        "Group: " + profile.groupName(),
                        "Enrollment year: "
                                + profile.enrollmentYear());

        return new ScheduleExportData(
                "Student",
                ownerName,
                details,
                schedule);
    }
}