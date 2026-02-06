package ua.foxminded.university.web.profile;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
	public String profile(@AuthenticationPrincipal UserDetails principal, Model model) {
	    var roleKey = principalHandler.getRole(principal);
	    var userId = principalHandler.parseUserId(principal);

	    return switch (roleKey) {
	    
		case "admin" -> {
			model.addAttribute("profile", appUserService.getAdminProfileView(userId));
			yield "profile/admin";
		}
		
		case "teacher" -> {
			model.addAttribute("profile", teacherService.getTeacherProfileView(userId));
			yield "profile/teacher";
		}
		
		case "student" -> {
			model.addAttribute("profile", studentService.getStudentProfileView(userId));
			yield "profile/student";
		}
		
		default ->
			throw new AccessDeniedException("Unsupported role=" + roleKey + " for userId=" + principal.getUsername());
			
	    };
	}

	@PostMapping("/profile/self/update")
	public String updateSelf(@AuthenticationPrincipal UserDetails principal,
	                         @ModelAttribute SelfUpdateForm form,
	                         RedirectAttributes ra) {

	    appUserService.updateProfileFields(new AppUserSelfUpdateDto(
	        principalHandler.parseUserId(principal),
	        fieldNormalizer.normalizeEmail(form.email()),
	        fieldNormalizer.normalizeField(form.firstName()),
	        fieldNormalizer.normalizeField(form.lastName())
	    ));

	    ra.addFlashAttribute("updated", true);
	    
	    return "redirect:/profile";
	}
}
