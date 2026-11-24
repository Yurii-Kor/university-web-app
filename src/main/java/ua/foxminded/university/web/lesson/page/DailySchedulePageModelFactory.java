package ua.foxminded.university.web.lesson.page;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import ua.foxminded.university.web.util.PrincipalHandler;

@Component
public class DailySchedulePageModelFactory {
    
    private static final long NEXT_DAY_OFFSET = 1L;

    private final PrincipalHandler principalHandler;
    private final Map<SchedulePageMode, SchedulePageStrategy> byMode;
    private final Clock clock;

    public DailySchedulePageModelFactory(
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

    public DailySchedulePageModel build(
            UserDetails principal,
            LocalDate requestedDate) {

        var roleKey = principalHandler.getRole(principal);
        var userId = principalHandler.parseUserId(principal);
        var mode = SchedulePageMode.fromRoleKey(roleKey);

        var strategy = Optional.ofNullable(
                byMode.get(mode))
                .orElseThrow(() ->
                        new AccessDeniedException(
                                "No schedule strategy configured for mode=%s, userId=%d"
                                        .formatted(mode, userId)));

        var today = LocalDate.now(clock);
        var date = Optional.ofNullable(requestedDate).orElse(today);

        var from = startOfDay(date);
        var to = startOfDay(date.plusDays(NEXT_DAY_OFFSET));

        var lessons =
                strategy.loadLessons(
                        userId,
                        from,
                        to);

        return mode.toDailyPage(
                date,
                date.equals(today),
                lessons);
    }

    private OffsetDateTime startOfDay(
            LocalDate date) {

        return date.atStartOfDay(
                clock.getZone())
                .toOffsetDateTime();
    }
}