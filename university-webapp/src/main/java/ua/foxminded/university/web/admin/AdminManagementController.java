package ua.foxminded.university.web.admin;

import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ua.foxminded.university.service.AppUserService;
import ua.foxminded.university.web.admin.dto.AdminCreateForm;
import ua.foxminded.university.web.admin.dto.AdminCreateFormMapper;
import ua.foxminded.university.web.admin.validation.AdminCreateFormValidator;
import ua.foxminded.university.web.bind.TrimToNullLowercaseEditor;
import ua.foxminded.university.web.util.PrincipalHandler;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminManagementController {

    private final AppUserService appUserService;
    private final PrincipalHandler principalHandler;
    private final AdminCreateFormMapper mapper;
    private final AdminCreateFormValidator adminCreateFormValidator;

    @InitBinder("form")
    void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, "email", new TrimToNullLowercaseEditor());
        binder.registerCustomEditor(String.class, "firstName", new StringTrimmerEditor(true));
        binder.registerCustomEditor(String.class, "lastName", new StringTrimmerEditor(true));

        binder.addValidators(adminCreateFormValidator);
    }

    @GetMapping
    public String adminsPage(@AuthenticationPrincipal UserDetails principal, Model model) {
        var selfId = principalHandler.parseUserId(principal);
        var admins = appUserService.listAdminsForView(selfId);

        model.addAttribute("admins", admins);
        model.addAttribute("selfId", selfId);

        return "admin/admins";
    }

    @GetMapping("/create")
    public String createAdminPage(@ModelAttribute("form") AdminCreateForm form) {
        return "admin/create";
    }

    @PostMapping("/create")
    public String createAdmin(@Valid @ModelAttribute("form") AdminCreateForm form,
                              BindingResult br,
                              RedirectAttributes ra) {

        if (br.hasErrors()) {
            return "admin/create";
        }

        var dto = mapper.toCreateDto(form);
        appUserService.createAdmin(dto);

        ra.addFlashAttribute("created", true);
        return "redirect:/admin";
    }

    @PostMapping("/{id}/enable")
    public String enableAdmin(@AuthenticationPrincipal UserDetails principal,
                              @PathVariable("id") long id,
                              RedirectAttributes ra) {

        var selfId = principalHandler.parseUserId(principal);
        assertNotSelf(id, selfId);

        appUserService.enableUserByIds(id);

        ra.addFlashAttribute("ok", "Admin enabled.");
        return "redirect:/admin";
    }

    @PostMapping("/{id}/disable")
    public String disableAdmin(@AuthenticationPrincipal UserDetails principal,
                               @PathVariable("id") long id,
                               RedirectAttributes ra) {

        var selfId = principalHandler.parseUserId(principal);
        assertNotSelf(id, selfId);

        appUserService.disableUserByIds(id);

        ra.addFlashAttribute("ok", "Admin disabled.");
        return "redirect:/admin";
    }

    @PostMapping("/{id}/delete")
    public String deleteAdmin(@AuthenticationPrincipal UserDetails principal,
                              @PathVariable("id") long id,
                              RedirectAttributes ra) {

        var selfId = principalHandler.parseUserId(principal);
        assertNotSelf(id, selfId);

        appUserService.deleteAdmin(id);

        return "redirect:/admin";
    }

    private void assertNotSelf(long targetId, long selfId) {
        if (targetId == selfId) {
            throw new IllegalStateException("You cannot modify your own admin account here.");
        }
    }
}