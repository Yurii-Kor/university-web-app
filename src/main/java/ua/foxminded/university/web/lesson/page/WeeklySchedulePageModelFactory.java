package ua.foxminded.university.web.lesson.page;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import ua.foxminded.university.model.persistence.lesson.projection.LessonScheduleItemView;
import ua.foxminded.university.web.util.PrincipalHandler;

@Component
public class WeeklySchedulePageModelFactory {

    private static final int DAYS_IN_WEEK = 7;
    private static final long LAST_DAY_OF_WEEK_OFFSET = DAYS_IN_WEEK - 1L;
    private static final long NEXT_WEEK_OFFSET = 1L;

    private final PrincipalHandler principalHandler;
    private final Map<SchedulePageMode, SchedulePageStrategy> byMode;
    private final Clock clock;

    public WeeklySchedulePageModelFactory(
            PrincipalHandler principalHandler,
            List<SchedulePageStrategy> strategies,
            Clock clock) {

        this.principalHandler = principalHandler;
        this.clock = clock;
        this.byMode = strategies.stream()
                .collect(Collectors.toMap(
                        SchedulePageStrategy::mode,
                        strategy -> strategy));
    }

    public WeeklySchedulePageModel build(
            UserDetails principal,
            LocalDate requestedDate) {

        var roleKey = principalHandler.getRole(principal);
        var userId = principalHandler.parseUserId(principal);
        var mode = SchedulePageMode.fromRoleKey(roleKey);

        var strategy = Optional.ofNullable(
                byMode.get(mode))
                .orElseThrow(() ->
                        new AccessDeniedException(
                                "No weekly schedule strategy configured for mode=%s, userId=%d"
                                        .formatted(
                                                mode,
                                                userId)));

        var today = LocalDate.now(clock);
        var anchorDate = Optional.ofNullable(requestedDate)
                .orElse(today);

        var weekStart = anchorDate.with(
                TemporalAdjusters.previousOrSame(
                        DayOfWeek.MONDAY));

        var weekEnd = weekStart.plusDays(
                LAST_DAY_OF_WEEK_OFFSET);

        var from = startOfDay(weekStart);
        var to = startOfDay(
                weekStart.plusWeeks(
                        NEXT_WEEK_OFFSET));

        var lessons = strategy.loadLessons(
                userId,
                from,
                to);

        var lessonsByDay = lessons.stream()
                .collect(Collectors.groupingBy(
                        this::localDate));

        var days = IntStream.range(
                0,
                DAYS_IN_WEEK)
                .mapToObj(index -> {
                    var date = weekStart.plusDays(index);

                    return new ScheduleDayModel(
                            date,
                            date.equals(today),
                            List.copyOf(
                                    lessonsByDay.getOrDefault(
                                            date,
                                            List.of())));
                })
                .toList();

        return mode.toPage(
                weekStart,
                weekEnd,
                days);
    }

    private OffsetDateTime startOfDay(
            LocalDate date) {

        return date.atStartOfDay(
                clock.getZone())
                .toOffsetDateTime();
    }

    private LocalDate localDate(
            LessonScheduleItemView lesson) {

        return lesson.startTime()
                .atZoneSameInstant(
                        clock.getZone())
                .toLocalDate();
    }
}