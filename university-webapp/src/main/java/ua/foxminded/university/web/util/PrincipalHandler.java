package ua.foxminded.university.web.util;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;

@Component
public class PrincipalHandler {

	private static String ROLE_PREFIX = "ROLE_";
	private static int ONLY_ONE_ROLE = 1;
	
	public boolean isAuthenticated(Authentication auth) {
		return Optional.ofNullable(auth)
	            .filter(Authentication::isAuthenticated)
	            .filter(a -> !(a instanceof AnonymousAuthenticationToken))
	            .isPresent();
	}

	public long parseUserId(UserDetails principal) {
		requirePrincipal(principal);
		
		try {
			return Long.parseLong(principal.getUsername());
		} catch (Exception e) {
			throw new AuthenticationCredentialsNotFoundException(
					"Invalid principal username (expected numeric userId), got: " + principal.getUsername());
		}
	}

	public String getRole(UserDetails principal) {
		assertHasExactlyOneRole(principal);

		return getRoles(principal).getFirst().substring(ROLE_PREFIX.length()).toLowerCase(Locale.ROOT);
	}

	public void requirePrincipal(UserDetails principal) {
		assertHasPrincipal(principal);
		assertHasUsername(principal);
		assertHasAuthorities(principal);
		assertHasExactlyOneRole(principal);

	}

	private void assertHasPrincipal(UserDetails principal) {
		Optional.ofNullable(principal).orElseThrow(() -> {
			return new AuthenticationCredentialsNotFoundException("No authenticated principal");
		});
	}

	private void assertHasUsername(UserDetails principal) {
		Optional.ofNullable(principal.getUsername()).map(String::trim).filter(u -> !u.isBlank()).orElseThrow(() -> {
			return new AuthenticationCredentialsNotFoundException("No authenticated principal");
		});
	}

	private void assertHasAuthorities(UserDetails principal) {
		Optional.ofNullable(principal.getAuthorities()).orElseThrow(() -> {
			return new AuthenticationCredentialsNotFoundException("No role for user : " + principal.getUsername());
		});
	}

	private void assertHasExactlyOneRole(UserDetails principal) {
		var roles = getRoles(principal);

		if (roles.size() != ONLY_ONE_ROLE) {
			throw new AuthenticationCredentialsNotFoundException(
					"Expected exactly 1 role for userId=" + principal.getUsername() + ", got: " + roles);
		}
	}

	private List<String> getRoles(UserDetails principal) {
		return principal.getAuthorities()
				.stream()
				.map(GrantedAuthority::getAuthority)
				.filter(Objects::nonNull)
				.filter(a -> a.startsWith(ROLE_PREFIX))
				.distinct()
				.toList();
	}
}
