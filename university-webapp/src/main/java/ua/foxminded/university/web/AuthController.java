package ua.foxminded.university.web;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import lombok.RequiredArgsConstructor;
import ua.foxminded.university.web.util.PrincipalHandler;

@Controller
@RequiredArgsConstructor
public class AuthController {
	
	private final PrincipalHandler principalHandler;

    @GetMapping("/login")
    public String loginPage(Authentication auth) {
    	return principalHandler.isAuthenticated(auth) ? "redirect:/profile" : "login";
    }
}
