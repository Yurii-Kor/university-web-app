package ua.foxminded.university.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
public class SecurityConfig {

	@Bean
	AuthenticationProvider authenticationProvider(UserDetailsService uds, PasswordEncoder encoder) {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider(uds);
		provider.setPasswordEncoder(encoder);

		return provider;
	}

	@Bean
	SecurityFilterChain filterChain(HttpSecurity http, AuthenticationProvider authProvider) throws Exception {
		var logoutHandler = new SecurityContextLogoutHandler();

		http.csrf(Customizer.withDefaults())

				.authorizeHttpRequests(reg -> reg.requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()
						.requestMatchers("/").permitAll()
						.requestMatchers("/login").anonymous()
						.anyRequest().authenticated())

				.formLogin(form -> form.loginPage("/login")
						.loginProcessingUrl("/login")
						.defaultSuccessUrl("/profile", true)
						.failureUrl("/login?error"))

				.exceptionHandling(ex -> ex.authenticationEntryPoint((req, res, authEx) -> {
					doLogoutIfAuthenticated(req, res, logoutHandler);
					res.sendRedirect(req.getContextPath() + "/login?auth");
				}).accessDeniedHandler((req, res, deniedEx) -> {
					res.sendRedirect(resolveDeniedRedirect(req));
				}))

				.logout(logout -> logout.logoutUrl("/logout")
						.logoutSuccessUrl("/")
						.invalidateHttpSession(true)
						.clearAuthentication(true)
						.deleteCookies("JSESSIONID"))

				.authenticationProvider(authProvider)

				.httpBasic(Customizer.withDefaults());

		return http.build();
	}

	private void doLogoutIfAuthenticated(HttpServletRequest req, 
										 HttpServletResponse res,
										 SecurityContextLogoutHandler logoutHandler) {
		
		var auth = SecurityContextHolder.getContext().getAuthentication();

		var authenticated = auth != null && !(auth instanceof AnonymousAuthenticationToken) && auth.isAuthenticated();

		if (authenticated) {
			logoutHandler.logout(req, res, auth);
		}
	}
	
	private String resolveDeniedRedirect(HttpServletRequest req) {
		String ctx = req.getContextPath();
		String uri = req.getRequestURI();

		if ((ctx + "/login").equals(uri)) {
			return ctx + "/profile";
		}

		return ctx + "/profile?denied";
	}
}
