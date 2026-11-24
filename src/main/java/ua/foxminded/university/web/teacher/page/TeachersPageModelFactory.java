package ua.foxminded.university.web.teacher.page;

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
public class TeachersPageModelFactory {

    private static final int PAGE_SIZE = 6;

    private final PrincipalHandler principalHandler;
    private final Map<TeachersPageMode, TeachersPageStrategy> byMode;

    public TeachersPageModelFactory(
            PrincipalHandler principalHandler,
            List<TeachersPageStrategy> strategies) {

        this.principalHandler = principalHandler;

        this.byMode = strategies.stream()
                .collect(Collectors.toMap(
                        TeachersPageStrategy::mode,
                        strategy -> strategy));
    }

    public TeachersPageModel build(
            UserDetails principal,
            int requestedPage) {

        var roleKey = principalHandler.getRole(principal);
        var userId = principalHandler.parseUserId(principal);
        var mode = TeachersPageMode.fromRoleKey(roleKey);

        var strategy = Optional.ofNullable(
                byMode.get(mode))
                .orElseThrow(() ->
                        new AccessDeniedException(
                                "No teachers page strategy configured for mode="
                                        + mode
                                        + ", userId="
                                        + userId));

        int safePage = Math.max(requestedPage, 0);
        Pageable pageable = PageRequest.of(safePage, PAGE_SIZE);
        var teachersPage = strategy.loadTeachers(userId, pageable);

        return mode.toPage(teachersPage);
    }
}