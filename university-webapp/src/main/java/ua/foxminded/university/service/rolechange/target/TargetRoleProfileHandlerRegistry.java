package ua.foxminded.university.service.rolechange.target;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import ua.foxminded.university.model.domain.enums.UserRole;
import ua.foxminded.university.service.rolechange.target.strategy.TargetRoleProfileHandler;

@Component
public class TargetRoleProfileHandlerRegistry {

    private final Map<UserRole, TargetRoleProfileHandler> handlersByRole;

    public TargetRoleProfileHandlerRegistry(List<TargetRoleProfileHandler> handlers) {
        this.handlersByRole = handlers.stream()
                .collect(Collectors.toMap(TargetRoleProfileHandler::role, handler -> handler));
    }
    
    public Optional<Class<? extends TargetRoleProfileData>> targetDataTypeFor(UserRole role) {
        return getRequired(role).targetDataType();
    }

    public TargetRoleProfileHandler getRequired(UserRole role) {
        return Optional.ofNullable(handlersByRole.get(role))
                .orElseThrow(() -> new IllegalStateException(
                        "No target role profile handler configured for role=" + role));
    }
}