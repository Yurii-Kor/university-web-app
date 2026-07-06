package ua.foxminded.university.web.profile.page;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import ua.foxminded.university.web.util.PrincipalHandler;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class ProfilePageModelFactory {

    private final PrincipalHandler principalHandler;
    private final Map<String, ProfilePageStrategy> byRole;

    public ProfilePageModelFactory(PrincipalHandler principalHandler,
                                   List<ProfilePageStrategy> strategies) {
    	
        this.principalHandler = principalHandler;
		this.byRole = strategies.stream().collect(Collectors.toMap(ProfilePageStrategy::roleKey, s -> s));
    }

    public ProfilePageModel build(UserDetails principal) {
        var roleKey = principalHandler.getRole(principal);
        var userId = principalHandler.parseUserId(principal);

		var strategy = Optional.ofNullable(byRole.get(roleKey))
				.orElseThrow(() -> new AccessDeniedException("Unsupported role=" + roleKey + " for userId=" + userId));

        return strategy.build(userId);
    }
}