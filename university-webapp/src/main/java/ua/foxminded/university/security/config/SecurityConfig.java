package ua.foxminded.university.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import ua.foxminded.university.security.web.LoginAuthEntryPoint;
import ua.foxminded.university.security.web.LoginFailureHandler;
import ua.foxminded.university.security.web.LogoutToLoginSuccessHandler;
import ua.foxminded.university.security.web.ProfileAccessDeniedHandler;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
	
	private static final String SESSION_NAME ="JSESSIONID";

	@Bean
	AuthenticationProvider authenticationProvider(UserDetailsService uds, PasswordEncoder encoder) {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider(uds);
		provider.setPasswordEncoder(encoder);

		return provider;
	}

	@Bean
	SecurityFilterChain filterChain(HttpSecurity http, 
									AuthenticationProvider authProvider, 
									LoginFailureHandler failureHandler, 
									LoginAuthEntryPoint loginAuthEntryPoint,
									ProfileAccessDeniedHandler profileAccessDeniedHandler,
									LogoutToLoginSuccessHandler logoutSuccessHandler) throws Exception {
		
		http.csrf(Customizer.withDefaults())

				.authorizeHttpRequests(reg -> reg.requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()
						.requestMatchers("/").permitAll()
						.requestMatchers("/login").anonymous()
						.anyRequest().authenticated())

				.formLogin(form -> form.loginPage("/login")
						.loginProcessingUrl("/login")
						.defaultSuccessUrl("/profile", true)
						.failureHandler(failureHandler))

				.exceptionHandling(ex -> ex
						.authenticationEntryPoint(loginAuthEntryPoint)
						.accessDeniedHandler(profileAccessDeniedHandler))

				.logout(logout -> logout.logoutUrl("/logout")
					    .logoutSuccessHandler(logoutSuccessHandler)
						.invalidateHttpSession(true)
						.clearAuthentication(true)
						.deleteCookies(SESSION_NAME))

				.authenticationProvider(authProvider)

				.httpBasic(Customizer.withDefaults());

		return http.build();
	}
}
