package ua.foxminded.university.web.group.details;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import ua.foxminded.university.web.util.PrincipalHandler;

@Component
public class GroupDetailsPageModelFactory {

    private final PrincipalHandler principalHandler;
    private final Map<GroupDetailsPageMode, GroupDetailsPageStrategy> byMode;

    public GroupDetailsPageModelFactory(
            PrincipalHandler principalHandler,
            List<GroupDetailsPageStrategy> strategies) {

        this.principalHandler = principalHandler;

        this.byMode = strategies.stream()
                .collect(Collectors.toMap(
                        GroupDetailsPageStrategy::mode,
                        strategy -> strategy));
    }

    public GroupDetailsPageResult build(
            UserDetails principal) {

        var roleKey = principalHandler.getRole(
                principal);

        var userId = principalHandler.parseUserId(
                principal);

        var mode = GroupDetailsPageMode.fromRoleKey(
                roleKey);

        var strategy = Optional.ofNullable(
                byMode.get(mode))
                .orElseThrow(() -> new AccessDeniedException(
                        "No group details strategy configured for mode="
                                + mode
                                + ", userId="
                                + userId));

        var page = strategy.buildPage(userId);

        return new GroupDetailsPageResult(
                mode.getViewName(),
                page);
    }
}