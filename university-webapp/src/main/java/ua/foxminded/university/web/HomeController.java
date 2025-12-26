package ua.foxminded.university.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

	@GetMapping("/")
	public String home(@AuthenticationPrincipal User user, Model model) {
		model.addAttribute("email", user.getUsername());
		model.addAttribute("authorities", user.getAuthorities());
		return "home";
	}

	@GetMapping("/profile")
	public String profile(@AuthenticationPrincipal User user, Model model) {
		model.addAttribute("email", user.getUsername());
		model.addAttribute("authorities", user.getAuthorities());
		return "profile";
	}
}
