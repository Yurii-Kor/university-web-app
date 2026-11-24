package ua.foxminded.university.web.util;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
public class PrincipalHandler {

    private static final String ROLE_PREFIX = "ROLE_";
    private static final int EXPECTED_ROLE_COUNT = 1;

    public long parseUserId(UserDetails principal) {
        var username = getUsername(principal);

        try {
            return Long.parseLong(username);
        } catch (NumberFormatException e) {
            throw new AuthenticationCredentialsNotFoundException(
                    "Invalid principal username "
                            + "(expected numeric userId), got: "
                            + username,
                    e);
        }
    }

    public String getRole(UserDetails principal) {
        requirePrincipal(principal);

        var roles = principal.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .filter(Objects::nonNull)
                .filter(authority ->
                        authority.startsWith(ROLE_PREFIX))
                .distinct()
                .toList();

        if (roles.size() != EXPECTED_ROLE_COUNT) {
            throw new AuthenticationCredentialsNotFoundException(
                    "Expected exactly 1 role for userId="
                            + getUsername(principal)
                            + ", got: "
                            + roles);
        }

        return roles.getFirst()
                .substring(ROLE_PREFIX.length())
                .toLowerCase(Locale.ROOT);
    }

    private String getUsername(UserDetails principal) {
        requirePrincipal(principal);

        return Optional.ofNullable(principal.getUsername())
                .map(String::trim)
                .filter(username -> !username.isBlank())
                .orElseThrow(() ->
                        new AuthenticationCredentialsNotFoundException(
                                "No Username principal"));
    }

    private void requirePrincipal(UserDetails principal) {
        Optional.ofNullable(principal.getUsername())
                .orElseThrow(() ->
                new AuthenticationCredentialsNotFoundException(
                        "No authenticated principal"));
        
    }
}