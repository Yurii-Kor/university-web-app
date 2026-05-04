package ua.foxminded.university.service.rolechange.current;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import ua.foxminded.university.model.domain.enums.UserRole;
import ua.foxminded.university.service.rolechange.current.strategy.CurrentRoleProfileHandler;

@Component
public class CurrentRoleProfileHandlerRegistry {

    private final Map<UserRole, CurrentRoleProfileHandler> handlersByRole;

    public CurrentRoleProfileHandlerRegistry(List<CurrentRoleProfileHandler> handlers) {
        this.handlersByRole = handlers.stream()
                .collect(Collectors.toMap(CurrentRoleProfileHandler::role, handler -> handler));
    }

    public CurrentRoleProfileHandler getRequired(UserRole role) {
        return Optional.ofNullable(handlersByRole.get(role))
                .orElseThrow(() -> new IllegalStateException(
                        "No current role profile handler configured for role=" + role));
    }
}
