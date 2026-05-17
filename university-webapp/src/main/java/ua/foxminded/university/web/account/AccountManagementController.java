package ua.foxminded.university.web.account;

import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import lombok.RequiredArgsConstructor;
import ua.foxminded.university.model.domain.enums.AcademicRank;
import ua.foxminded.university.model.domain.enums.UserRole;
import ua.foxminded.university.service.AppUserService;
import ua.foxminded.university.service.StudentService;
import ua.foxminded.university.service.StudyGroupService;
import ua.foxminded.university.service.TeacherService;
import ua.foxminded.university.service.dto.request.rolechange.ToTeacherRoleChangeDto;
import ua.foxminded.university.service.dto.request.rolechange.ToStudentRoleChangeDto;
import ua.foxminded.university.service.rolechange.RoleChangeAssessmentService;
import ua.foxminded.university.service.rolechange.RoleChangeService;
import ua.foxminded.university.service.rolechange.assessment.RoleChangeAssessment;
import ua.foxminded.university.service.rolechange.target.TargetRoleProfileData;
import ua.foxminded.university.web.account.page.AccountsPageModelFactory;

@Controller
@RequestMapping("/accounts")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AccountManagementController {

    private final RoleChangeAssessmentService roleChangeAssessor;
    private final AccountsPageModelFactory pageFactory;
    private final RoleChangeService roleChangeService;

    private final StudyGroupService studyGroupService;
    private final AppUserService appUserService;
    private final StudentService studentService;
    private final TeacherService teacherService;

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

        return roleChangeAssessor.assessRoleChange(id, sourceRole, targetRole);
    }
    
    @PostMapping("/{id}/role-change")
    public String changeRole(@PathVariable long id,
                             @RequestParam UserRole sourceRole,
                             @RequestParam UserRole targetRole,
                             @ModelAttribute ToTeacherRoleChangeDto teacherData,
                             @ModelAttribute ToStudentRoleChangeDto studentData,
                             RedirectAttributes ra) {

        roleChangeService.changeRole(
                id,
                sourceRole,
                targetRole,
                targetDataFor(targetRole, teacherData, studentData)
        );

        ra.addFlashAttribute("ok", roleChangeSuccessMessage(sourceRole, targetRole));
        ra.addFlashAttribute("accountId", id);

        return redirectToRole(targetRole, 0);
    }
    
    @PostMapping("/{id}/role-change/restore")
    public String restoreRole(@PathVariable long id,
                              @RequestParam UserRole sourceRole,
                              @RequestParam UserRole targetRole,
                              RedirectAttributes ra) {

        roleChangeService.restoreRole(id, sourceRole, targetRole);

        ra.addFlashAttribute("ok", roleChangeSuccessMessage(sourceRole, targetRole));
        ra.addFlashAttribute("accountId", id);

        return redirectToRole(targetRole, 0);
    }

    @PostMapping("/{id}/enable")
    public String enableAccount(@PathVariable long id,
                                @RequestParam UserRole role,
                                @RequestParam(name = "page", defaultValue = "0") int pageNumber,
                                RedirectAttributes ra) {
        
        assertRoleSupportsAccountAction(role);

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

        assertRoleSupportsAccountAction(role);

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

        assertRoleSupportsAccountAction(role);

        deleteAccountByRole(id, role);

        ra.addFlashAttribute("ok", roleLabel(role) + " account deleted.");
        ra.addFlashAttribute("accountId", id);

        return redirectToRole(role, pageNumber);
    }

    private TargetRoleProfileData targetDataFor(UserRole targetRole,
                                                ToTeacherRoleChangeDto teacherData,
                                                ToStudentRoleChangeDto studentData) {
        
        return switch (targetRole) {
            case TEACHER -> teacherData;
            case STUDENT -> studentData;
            case ADMIN   -> throw new IllegalArgumentException(
                    "Changing account role to ADMIN is not supported from this workflow.");
        };
    }
    
    private String redirectToRole(UserRole role, int pageNumber) {
        return "redirect:/accounts?view=" + viewOf(role) + "&page=" + Math.max(pageNumber, 0);
    }
    
    private String viewOf(UserRole role) {
        return switch (role) {
            case STUDENT -> "students";
            case TEACHER -> "teachers";
            case ADMIN -> throw new IllegalArgumentException("Admin accounts view is not supported here.");
        };
    }
    
    private String roleChangeSuccessMessage(UserRole sourceRole, UserRole targetRole) {
        return roleLabel(sourceRole) + " role changed to " + roleLabel(targetRole) + ".";
    }
    
    private String roleLabel(UserRole role) {
        return switch (role) {
            case STUDENT -> "Student";
            case TEACHER -> "Teacher";
            case ADMIN -> throw new IllegalArgumentException(
                    "Admin accounts view is not supported here.");
        };
    }
    
    private void assertRoleSupportsAccountAction(UserRole role) {
        if (role == UserRole.STUDENT || role == UserRole.TEACHER) {
            return;
        }

        throw new IllegalArgumentException("Only student and teacher account actions are supported.");
    }
    
    private void deleteAccountByRole(long id, UserRole role) {
        switch (role) {
            case STUDENT -> studentService.deleteById(id);
            case TEACHER -> teacherService.deleteById(id);
            case ADMIN -> throw new IllegalArgumentException(
                    "Admin account deletion is not supported here.");
        }
    }
}