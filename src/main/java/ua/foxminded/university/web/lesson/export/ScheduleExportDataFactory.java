package ua.foxminded.university.web.lesson.export;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import ua.foxminded.university.web.lesson.page.SchedulePageMode;
import ua.foxminded.university.web.lesson.page.WeeklySchedulePageModelFactory;
import ua.foxminded.university.web.util.PrincipalHandler;

@Component
public class ScheduleExportDataFactory {

    private final WeeklySchedulePageModelFactory weeklyPageFactory;
    private final PrincipalHandler principalHandler;
    private final Map<SchedulePageMode, ScheduleExportStrategy> byMode;

    public ScheduleExportDataFactory(
            WeeklySchedulePageModelFactory weeklyPageFactory,
            PrincipalHandler principalHandler,
            List<ScheduleExportStrategy> strategies) {

        this.weeklyPageFactory = weeklyPageFactory;
        this.principalHandler = principalHandler;

        this.byMode =
                strategies.stream()
                        .collect(
                                Collectors.toMap(
                                        ScheduleExportStrategy::mode,
                                        strategy -> strategy));
    }

    public ScheduleExportData build(
            UserDetails principal,
            LocalDate requestedDate) {

        var roleKey =
                principalHandler.getRole(
                        principal);

        var userId =
                principalHandler.parseUserId(
                        principal);

        var mode =
                SchedulePageMode.fromRoleKey(
                        roleKey);

        var strategy =
                Optional.ofNullable(
                        byMode.get(mode))
                        .orElseThrow(() ->
                                new AccessDeniedException(
                                        "No schedule export strategy configured for mode=%s, userId=%d"
                                                .formatted(
                                                        mode,
                                                        userId)));

        var schedule =
                weeklyPageFactory.build(
                        principal,
                        requestedDate);

        return strategy.build(
                userId,
                schedule);
    }
}