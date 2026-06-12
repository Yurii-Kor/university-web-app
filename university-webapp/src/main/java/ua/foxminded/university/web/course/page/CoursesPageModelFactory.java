package ua.foxminded.university.web.course.page;

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
public class CoursesPageModelFactory {

    private static final int PAGE_SIZE = 6;

    private final PrincipalHandler principalHandler;
    private final Map<CoursesPageMode, CoursesPageStrategy> byMode;

    public CoursesPageModelFactory(PrincipalHandler principalHandler,
                                   List<CoursesPageStrategy> strategies) {
    	
        this.principalHandler = principalHandler;
		this.byMode = strategies.stream().collect(Collectors.toMap(CoursesPageStrategy::mode, s -> s));
    }

    public CoursesPageModel build(UserDetails principal, int requestedPage) {
        var roleKey = principalHandler.getRole(principal);
        var userId = principalHandler.parseUserId(principal);

        var mode = CoursesPageMode.fromRoleKey(roleKey);

		var strategy = Optional.ofNullable(byMode.get(mode))
				.orElseThrow(() -> new AccessDeniedException(
						"No courses page strategy configured for mode=" + mode + ", userId=" + userId));

        int safePage = Math.max(requestedPage, 0);
        Pageable pageable = PageRequest.of(safePage, PAGE_SIZE);

        var coursesPage = strategy.loadCourses(userId, pageable);
        return mode.toPage(coursesPage);
    }
}