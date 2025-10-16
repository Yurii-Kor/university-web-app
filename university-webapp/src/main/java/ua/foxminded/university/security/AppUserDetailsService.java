package ua.foxminded.university.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import ua.foxminded.university.model.domain.AppUser;
import ua.foxminded.university.model.repository.AppUserRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {

	private final AppUserRepository usersRepository;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		String email = username == null ? null : username.toLowerCase();

		AppUser currentUser = usersRepository.findByEmailIgnoreCase(email)
				.orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

		var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + currentUser.getRole().name()));

		return User.withUsername(currentUser.getEmail())
				.password(currentUser.getPassword())
				.authorities(authorities)
				.disabled(!currentUser.isEnabled())
				.build();
	}
}
