package ua.foxminded.university.web.admin;

import java.util.Comparator;
import java.util.List;

import org.springframework.beans.propertyeditors.StringTrimmerEditor;
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
import ua.foxminded.university.model.repository.dto.AdminRowView;
import ua.foxminded.university.service.AppUserService;
import ua.foxminded.university.web.admin.dto.AdminCreateForm;
import ua.foxminded.university.web.admin.dto.AdminCreateFormMapper;
import ua.foxminded.university.web.admin.validation.AdminCreateFormValidator;
import ua.foxminded.university.web.bind.TrimToNullLowercaseEditor;
import ua.foxminded.university.web.util.PrincipalHandler;

@Controller
@RequestMapping("/admin")
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
        var admins = sortAdminsForView(appUserService.listAdmins(), selfId);

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
        appUserService.createAdmins(List.of(dto));

        ra.addFlashAttribute("created", true);
        return "redirect:/admin";
    }

    @PostMapping("/{id}/enable")
    public String enableAdmin(@AuthenticationPrincipal UserDetails principal,
                              @PathVariable("id") long id,
                              RedirectAttributes ra) {

        var selfId = principalHandler.parseUserId(principal);
        assertNotSelf(id, selfId);

        appUserService.enableUsersByIds(List.of(id));

        ra.addFlashAttribute("ok", "Admin enabled.");
        return "redirect:/admin";
    }

    @PostMapping("/{id}/disable")
    public String disableAdmin(@AuthenticationPrincipal UserDetails principal,
                               @PathVariable("id") long id,
                               RedirectAttributes ra) {

        var selfId = principalHandler.parseUserId(principal);
        assertNotSelf(id, selfId);

        appUserService.disableUsersByIds(List.of(id));

        ra.addFlashAttribute("ok", "Admin disabled.");
        return "redirect:/admin";
    }

    @PostMapping("/{id}/delete")
    public String deleteAdmin(@AuthenticationPrincipal UserDetails principal,
                              @PathVariable("id") long id,
                              RedirectAttributes ra) {

        var selfId = principalHandler.parseUserId(principal);
        assertNotSelf(id, selfId);

        var result = appUserService.deleteAdminsByIds(List.of(id));

        if (result.deletedIds().contains(id)) {
            ra.addFlashAttribute("ok", "Admin deleted.");
        } else if (result.notFoundIds().contains(id)) {
            ra.addFlashAttribute("err", "Admin not found.");
        } else {
            ra.addFlashAttribute("ok", "Nothing to delete.");
        }

        return "redirect:/admin";
    }

    private void assertNotSelf(long targetId, long selfId) {
        if (targetId == selfId) {
            throw new IllegalStateException("You cannot modify your own admin account here.");
        }
    }

    private List<AdminRowView> sortAdminsForView(List<AdminRowView> admins, long selfId) {
        return admins.stream()
                .sorted(Comparator
                        .comparingInt((AdminRowView a) -> a.id() != null && a.id() == selfId ? 0 : 1)
                        .thenComparingInt(a -> a.enabled() ? 0 : 1)
                        .thenComparing(AdminRowView::id, Comparator.nullsLast(Long::compareTo)))
                .toList();
    }
}