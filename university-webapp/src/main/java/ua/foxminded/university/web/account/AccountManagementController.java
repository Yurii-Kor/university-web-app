package ua.foxminded.university.web.account;

import java.util.Locale;
import java.util.Optional;

import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import lombok.RequiredArgsConstructor;
import ua.foxminded.university.model.domain.enums.AcademicRank;
import ua.foxminded.university.model.domain.enums.UserRole;
import ua.foxminded.university.service.AppUserService;
import ua.foxminded.university.service.StudyGroupService;
import ua.foxminded.university.service.dto.request.rolechange.ToStudentRoleChangeDto;
import ua.foxminded.university.service.dto.request.rolechange.ToTeacherRoleChangeDto;
import ua.foxminded.university.service.rolechange.RoleChangeFacade;
import ua.foxminded.university.service.rolechange.assessment.RoleChangeAssessment;
import ua.foxminded.university.web.account.delete.AccountDeleterRegistry;
import ua.foxminded.university.web.account.page.AccountsPageModelFactory;

@Controller
@RequestMapping("/accounts")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AccountManagementController {

    private final RoleChangeFacade roleChangeFacade;
    private final AccountsPageModelFactory pageFactory;
    
    private final AccountDeleterRegistry accountDeleterRegistry;

    private final StudyGroupService studyGroupService;
    private final AppUserService appUserService;

    @InitBinder
    void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    }

    @GetMapping
    public String accountsPage(@RequestParam(name = "view", defaultValue = "students") String view,
                               @RequestParam(name = "page", defaultValue = "0") int pageNumber,
                               Model model) {

        var page = pageFactory.build(view, pageNumber);

        model.addAttribute("page", page);
        model.addAttribute("all_groups", studyGroupService.listGroupOptions());
        model.addAttribute("all_ranks", AcademicRank.values());

        return "accounts/accounts";
    }

    @GetMapping("/{id}/role-change/assessment")
    @ResponseBody
    public RoleChangeAssessment assessRoleChange(@PathVariable long id,
                                                 @RequestParam UserRole sourceRole,
                                                 @RequestParam UserRole targetRole) {

        return roleChangeFacade.assessRoleChange(id, sourceRole, targetRole);
    }
    
    @PostMapping("/{id}/role-change/restore")
    public String restoreRole(@PathVariable long id,
                              @RequestParam UserRole sourceRole,
                              @RequestParam UserRole targetRole,
                              RedirectAttributes ra) {

        requireSupportedAccountRole(targetRole);

        roleChangeFacade.restoreRole(id, sourceRole, targetRole);

        ra.addFlashAttribute("ok", roleChangeSuccessMessage(sourceRole, targetRole));
        ra.addFlashAttribute("accountId", id);

        return redirectToRole(targetRole, 0);
    }

    @PostMapping(value = "/{id}/role-change", params = "targetRole=TEACHER")
    public String changeRoleToTeacher(@PathVariable long id,
                                      @RequestParam UserRole sourceRole,
                                      @Validated @ModelAttribute ToTeacherRoleChangeDto form,
                                      BindingResult bindingResult,
                                      RedirectAttributes ra) {

        if (rejectInvalidRoleChangeForm(id, bindingResult, ra)) {
            return redirectToRole(sourceRole, 0);
        }

        roleChangeFacade.changeRoleToTeacher(id, sourceRole, form);

        ra.addFlashAttribute("ok", roleChangeSuccessMessage(sourceRole, UserRole.TEACHER));
        ra.addFlashAttribute("accountId", id);

        return redirectToRole(UserRole.TEACHER, 0);
    }

    @PostMapping(value = "/{id}/role-change", params = "targetRole=STUDENT")
    public String changeRoleToStudent(@PathVariable long id,
                                      @RequestParam UserRole sourceRole,
                                      @Validated @ModelAttribute ToStudentRoleChangeDto form,
                                      BindingResult bindingResult,
                                      RedirectAttributes ra) {

        if (rejectInvalidRoleChangeForm(id, bindingResult, ra)) {
            return redirectToRole(sourceRole, 0);
        }

        roleChangeFacade.changeRoleToStudent(id, sourceRole, form);

        ra.addFlashAttribute("ok", roleChangeSuccessMessage(sourceRole, UserRole.STUDENT));
        ra.addFlashAttribute("accountId", id);

        return redirectToRole(UserRole.STUDENT, 0);
    }

    @PostMapping(value = "/{id}/role-change", params = "targetRole=ADMIN")
    public String changeRoleToAdmin() {
        throw new IllegalArgumentException(
                "Changing account role to ADMIN is not supported from this workflow.");
    }

    @PostMapping("/{id}/enable")
    public String enableAccount(@PathVariable long id,
                                @RequestParam UserRole role,
                                @RequestParam(name = "page", defaultValue = "0") int pageNumber,
                                RedirectAttributes ra) {
        
        requireSupportedAccountRole(role);

        appUserService.enableUserByIds(id);

        ra.addFlashAttribute("ok", roleLabel(role) + " account enabled.");
        ra.addFlashAttribute("accountId", id);

        return redirectToRole(role, pageNumber);
    }

    @PostMapping("/{id}/disable")
    public String disableAccount(@PathVariable long id,
                                 @RequestParam UserRole role,
                                 @RequestParam(name = "page", defaultValue = "0") int pageNumber,
                                 RedirectAttributes ra) {

        requireSupportedAccountRole(role);

        appUserService.disableUserByIds(id);

        ra.addFlashAttribute("ok", roleLabel(role) + " account disabled.");
        ra.addFlashAttribute("accountId", id);

        return redirectToRole(role, pageNumber);
    }

    @PostMapping("/{id}/delete")
    public String deleteAccount(@PathVariable long id,
                                @RequestParam UserRole role,
                                @RequestParam(name = "page", defaultValue = "0") int pageNumber,
                                RedirectAttributes ra) {

        accountDeleterRegistry.getRequired(role)
                .deleteByRoleAndId(role, id);

        ra.addFlashAttribute("ok", roleLabel(role) + " account deleted.");
        ra.addFlashAttribute("accountId", id);

        return redirectToRole(role, pageNumber);
    }

    private String redirectToRole(UserRole role, int pageNumber) {
        return "redirect:/accounts?view=" + viewOf(role) + "&page=" + Math.max(pageNumber, 0);
    }

    private String viewOf(UserRole role) {
        return requireSupportedAccountRole(role)
                .name()
                .toLowerCase(Locale.ROOT) + "s";
    }

    private String roleChangeSuccessMessage(UserRole sourceRole, UserRole targetRole) {
        return roleLabel(sourceRole) + " role changed to " + roleLabel(targetRole) + ".";
    }

    private String roleLabel(UserRole role) {
        var roleName = requireSupportedAccountRole(role)
                .name()
                .toLowerCase(Locale.ROOT);

        return Character.toUpperCase(roleName.charAt(0)) + roleName.substring(1);
    }

    private UserRole requireSupportedAccountRole(UserRole role) {
        return Optional.ofNullable(role)
                .filter(value -> value != UserRole.ADMIN)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Only student and teacher account actions are supported."));
    }
    
    private boolean rejectInvalidRoleChangeForm(long id,
                                                BindingResult bindingResult,
                                                RedirectAttributes ra) {

        if (!bindingResult.hasErrors()) {
            return false;
        }

        ra.addFlashAttribute("err", "Role change form contains invalid data.");
        ra.addFlashAttribute("accountId", id);

        return true;
    }
}