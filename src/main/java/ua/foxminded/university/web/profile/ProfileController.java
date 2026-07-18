package ua.foxminded.university.web.profile;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import ua.foxminded.university.service.appuser.AppUserService;
import ua.foxminded.university.service.appuser.dto.AppUserSelfUpdateDto;
import ua.foxminded.university.web.bind.TrimToNullLowercaseEditor;
import ua.foxminded.university.web.profile.dto.SelfUpdateForm;
import ua.foxminded.university.web.profile.page.ProfilePageModelFactory;
import ua.foxminded.university.web.util.PrincipalHandler;

@Controller
@RequiredArgsConstructor
public class ProfileController {

	private final AppUserService appUserService;
	private final PrincipalHandler principalHandler;
	private final ProfilePageModelFactory pageFactory;

	@GetMapping("/profile")
    public String profile(@AuthenticationPrincipal UserDetails principal, Model model) {
        var page = pageFactory.build(principal);
        
        model.addAttribute("profile", page.profile());
        return page.viewName();
    }
	
	@InitBinder("form")
    void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, "email", new TrimToNullLowercaseEditor());
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    }

	@PostMapping("/profile/self/update")
    public String updateSelf(@AuthenticationPrincipal UserDetails principal,
                             @Valid @ModelAttribute("form") SelfUpdateForm form,
                             BindingResult br,
                             Model model,
                             RedirectAttributes ra) {

        if (br.hasErrors()) return "redirect:/profile";

        var userId = principalHandler.parseUserId(principal);
        appUserService.updateProfileFields(new AppUserSelfUpdateDto(
                userId,
                form.email(),
                form.firstName(),
                form.lastName()
        ));

        ra.addFlashAttribute("updated", true);
        return "redirect:/profile";
    }
}
