package ua.foxminded.university.web.student.cards;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import ua.foxminded.university.web.util.PrincipalHandler;

@Component
public class StudentsCardsPageModelFactory {

    private static final int PAGE_SIZE = 6;

    private final PrincipalHandler principalHandler;
    private final Map<StudentsCardsPageMode, StudentsCardsPageStrategy<?>> byMode;

    public StudentsCardsPageModelFactory(
            PrincipalHandler principalHandler,
            List<StudentsCardsPageStrategy<?>> strategies) {

        this.principalHandler = principalHandler;

        this.byMode = strategies.stream()
                .collect(Collectors.toMap(
                        StudentsCardsPageStrategy::mode,
                        strategy -> strategy));
    }

    public StudentsCardsPageModel build(
            UserDetails principal,
            int requestedPage) {

        var roleKey = principalHandler.getRole(
                principal);

        var userId = principalHandler.parseUserId(
                principal);

        var mode = StudentsCardsPageMode.fromRoleKey(
                roleKey);

        var strategy = Optional.ofNullable(
                byMode.get(mode))
                .orElseThrow(() -> new AccessDeniedException(
                        "No students cards strategy configured for mode="
                                + mode
                                + ", userId="
                                + userId));

        var safePage = Math.max(
                requestedPage,
                0);

        Pageable pageable = PageRequest.of(
                safePage,
                PAGE_SIZE);

        return strategy.buildPage(
                userId,
                pageable);
    }
}