package ua.foxminded.university.web.profile;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import ua.foxminded.university.service.appuser.AppUserService;
import ua.foxminded.university.service.appuser.dto.AppUserPasswordChangeDto;
import ua.foxminded.university.web.profile.validation.PasswordChangeFormValidator;
import ua.foxminded.university.web.util.PrincipalHandler;

@Controller
@RequiredArgsConstructor
public class PasswordChangeController {

    private final AppUserService appUserService;
    private final PrincipalHandler principalHandler;
    private final PasswordChangeFormValidator passwordChangeFormValidator;

    @InitBinder("passwordForm")
    void initPasswordBinder(WebDataBinder binder) {
        binder.addValidators(passwordChangeFormValidator);
    }

    @GetMapping("/profile/password")
    public String passwordPage() {
        return "profile/password";
    }

    @PostMapping("/profile/password")
    public String changePassword(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @ModelAttribute("passwordForm") AppUserPasswordChangeDto form,
            RedirectAttributes ra) {

        var userId = principalHandler.parseUserId(principal);
        appUserService.changePasswordSelf(userId, form);

        ra.addFlashAttribute("passwordUpdated", true);
        return "redirect:/profile";
    }
}