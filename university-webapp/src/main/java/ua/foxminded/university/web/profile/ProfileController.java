package ua.foxminded.university.web.profile;

import lombok.RequiredArgsConstructor;

import java.util.Optional;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.RequestContextUtils;

import jakarta.servlet.http.HttpServletRequest;
import ua.foxminded.university.service.AppUserService;
import ua.foxminded.university.service.StudentService;
import ua.foxminded.university.service.TeacherService;
import ua.foxminded.university.service.dto.request.appuser.AppUserSelfUpdateDto;
import ua.foxminded.university.web.profile.dto.SelfUpdateForm;
import ua.foxminded.university.web.util.DtoFieldNormalizer;
import ua.foxminded.university.web.util.PrincipalHandler;

@Controller
@RequiredArgsConstructor
public class ProfileController {

	private final AppUserService appUserService;
	private final StudentService studentService;
	private final TeacherService teacherService;
	
	private final PrincipalHandler principalHandler;
	private final DtoFieldNormalizer fieldNormalizer;

	@GetMapping("/profile")
	public String profileRouter(@AuthenticationPrincipal UserDetails principal, 
								HttpServletRequest request,
								RedirectAttributes ra) {

		principalHandler.requirePrincipal(principal);
		var roleKey = principalHandler.getRole(principal);

		var param = Optional.ofNullable(request.getQueryString())
				.filter(qs -> !qs.isBlank())
				.map(qs -> "?" + qs)
				.orElse("");

		Optional.ofNullable(RequestContextUtils.getInputFlashMap(request))
				.ifPresent(flash -> flash.forEach(ra::addFlashAttribute));

		return switch (roleKey) {
			case "admin", "teacher", "student" -> "redirect:/profile/" + roleKey + param;
			default ->
				throw new AccessDeniedException("Unsupported role=" + roleKey + " for userId=" + principal.getUsername());
		};
	}

	@GetMapping("/profile/admin")
	public String adminProfile(@AuthenticationPrincipal UserDetails principal, Model model) {
		var userId = principalHandler.parseUserId(principal);
		model.addAttribute("profile", appUserService.getAdminProfileView(userId));
		
		return "profile/admin";
	}

	@GetMapping("/profile/teacher")
	public String teacherProfile(@AuthenticationPrincipal UserDetails principal, Model model) {
		var userId = principalHandler.parseUserId(principal);
		model.addAttribute("profile", teacherService.getTeacherProfileView(userId));
		
		return "profile/teacher";
	}

	@GetMapping("/profile/student")
	public String studentProfile(@AuthenticationPrincipal UserDetails principal, Model model) {
		var userId = principalHandler.parseUserId(principal);
		model.addAttribute("profile", studentService.getStudentProfileView(userId));
		
		return "profile/student";
	}

	@PostMapping("/profile/self/update")
	public String updateSelf(@AuthenticationPrincipal UserDetails principal, @ModelAttribute SelfUpdateForm form) {        
        appUserService.updateProfileFields(new AppUserSelfUpdateDto(
        		principalHandler.parseUserId(principal),
        		fieldNormalizer.normalizeEmail(form.email()),
        		fieldNormalizer.normalizeField(form.firstName()),
        		fieldNormalizer.normalizeField(form.lastName())
        ));

		return "redirect:/profile?updated";
	}
}
