package ua.foxminded.university.web.course.page;

import org.springframework.security.access.AccessDeniedException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import ua.foxminded.university.web.util.PrincipalHandler;

@Component
public class CoursesPageModelFactory {

    private final PrincipalHandler principalHandler;
    private final Map<String, CoursesPageStrategy> byRole;

    public CoursesPageModelFactory(PrincipalHandler principalHandler,
                                   List<CoursesPageStrategy> strategies) {
        this.principalHandler = principalHandler;
		this.byRole = strategies.stream().collect(Collectors.toMap(CoursesPageStrategy::roleKey, s -> s));
	}

    public CoursesPageModel build(UserDetails principal) {
        var roleKey = principalHandler.getRole(principal);
        var userId = principalHandler.parseUserId(principal);

		var strategy = Optional.ofNullable(byRole.get(roleKey))
				.orElseThrow(() -> new AccessDeniedException("Unsupported role=" + roleKey + " for userId=" + userId));

        return strategy.build(userId);
    }
}
