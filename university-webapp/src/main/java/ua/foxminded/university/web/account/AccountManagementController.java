package ua.foxminded.university.web.account;

import java.util.Objects;

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
import ua.foxminded.university.service.rolechange.RoleChangePlanningService;
import ua.foxminded.university.service.rolechange.plan.RoleChangePlan;
import ua.foxminded.university.service.rolechange.RoleChangeService;
import ua.foxminded.university.web.account.page.AccountsPageModelFactory;

@Controller
@RequestMapping("/accounts")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AccountManagementController {

    private final RoleChangePlanningService roleChangePlanningService;
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
    
    @GetMapping("/students/{id}/role-change/to-teacher/plan")
    @ResponseBody
    public RoleChangePlan planStudentToTeacher(@PathVariable long id) {
        return roleChangePlanningService.planRoleChange(id, UserRole.STUDENT, UserRole.TEACHER);
    }
    
    @GetMapping("/teachers/{id}/role-change/to-student/plan")
    @ResponseBody
    public RoleChangePlan planTeacherToStudent(@PathVariable long id) {
        return roleChangePlanningService.planRoleChange(id, UserRole.TEACHER, UserRole.STUDENT);
    }
    
    @PostMapping("/students/{id}/role-change/to-teacher")
    public String changeStudentToTeacher(@PathVariable long id,
                                         @ModelAttribute ToTeacherRoleChangeDto form,
                                         RedirectAttributes ra) {

        assertPathMatchesFormUserId(id, form.userId());

        roleChangeService.changeStudentToTeacher(form);

        ra.addFlashAttribute("ok", "Student role changed to teacher.");
        ra.addFlashAttribute("accountId", id);

        return redirectToTeachers(0);
    }
    
    @PostMapping("/students/{id}/role-change/to-teacher/restore")
    public String restoreStudentToTeacher(@PathVariable long id,
                                          RedirectAttributes ra) {

        roleChangeService.restoreStudentToTeacher(id);

        ra.addFlashAttribute("ok", "Student role changed to teacher.");
        ra.addFlashAttribute("accountId", id);

        return redirectToTeachers(0);
    }
    
    @PostMapping("/teachers/{id}/role-change/to-student")
    public String changeTeacherToStudent(@PathVariable long id,
                                         @ModelAttribute ToStudentRoleChangeDto form,
                                         RedirectAttributes ra) {

        assertPathMatchesFormUserId(id, form.userId());

        roleChangeService.changeTeacherToStudent(form);

        ra.addFlashAttribute("ok", "Teacher role changed to student.");
        ra.addFlashAttribute("accountId", id);

        return redirectToStudents(0);
    }
    
    @PostMapping("/teachers/{id}/role-change/to-student/restore")
    public String restoreTeacherToStudent(@PathVariable long id,
                                          RedirectAttributes ra) {

        roleChangeService.restoreTeacherToStudent(id);

        ra.addFlashAttribute("ok", "Teacher role changed to student.");
        ra.addFlashAttribute("accountId", id);

        return redirectToStudents(0);
    }

    @PostMapping("/students/{id}/enable")
    public String enableStudentAccount(@PathVariable long id,
                                       @RequestParam(name = "page", defaultValue = "0") int pageNumber,
                                       RedirectAttributes ra) {
        
        appUserService.enableUserByIds(id);

        ra.addFlashAttribute("ok", "Student account enabled.");
        ra.addFlashAttribute("accountId", id);

        return redirectToStudents(pageNumber);
    }

    @PostMapping("/students/{id}/disable")
    public String disableStudentAccount(@PathVariable long id,
                                        @RequestParam(name = "page", defaultValue = "0") int pageNumber,
                                        RedirectAttributes ra) {

        appUserService.disableUserByIds(id);

        ra.addFlashAttribute("ok", "Student account disabled.");
        ra.addFlashAttribute("accountId", id);

        return redirectToStudents(pageNumber);
    }

    @PostMapping("/teachers/{id}/disable")
    public String disableTeacherAccount(@PathVariable long id,
                                        @RequestParam(name = "page", defaultValue = "0") int pageNumber,
                                        RedirectAttributes ra) {

        appUserService.disableUserByIds(id);

        ra.addFlashAttribute("ok", "Teacher account disabled.");
        ra.addFlashAttribute("accountId", id);

        return redirectToTeachers(pageNumber);
    }

    @PostMapping("/teachers/{id}/enable")
    public String enableTeacherAccount(@PathVariable long id,
                                       @RequestParam(name = "page", defaultValue = "0") int pageNumber,
                                       RedirectAttributes ra) {
        
        appUserService.enableUserByIds(id);

        ra.addFlashAttribute("ok", "Teacher account enabled.");
        ra.addFlashAttribute("accountId", id);

        return redirectToTeachers(pageNumber);
    }

    @PostMapping("/students/{id}/delete")
    public String deleteStudentAccount(@PathVariable long id,
                                       @RequestParam(name = "page", defaultValue = "0") int pageNumber,
                                       RedirectAttributes ra) {

        studentService.deleteById(id);

        ra.addFlashAttribute("ok", "Student account deleted.");
        ra.addFlashAttribute("accountId", id);

        return redirectToStudents(pageNumber);
    }

    @PostMapping("/teachers/{id}/delete")
    public String deleteTeacherAccount(@PathVariable long id,
                                       @RequestParam(name = "page", defaultValue = "0") int pageNumber,
                                       RedirectAttributes ra) {

        teacherService.deleteById(id);

        ra.addFlashAttribute("ok", "Teacher account deleted.");
        ra.addFlashAttribute("accountId", id);

        return redirectToTeachers(pageNumber);
    }

    private String redirectToStudents(int pageNumber) {
        return "redirect:/accounts?view=students&page=" + normalizePage(pageNumber);
    }

    private String redirectToTeachers(int pageNumber) {
        return "redirect:/accounts?view=teachers&page=" + normalizePage(pageNumber);
    }

    private int normalizePage(int pageNumber) {
        return Math.max(pageNumber, 0);
    }

    private void assertPathMatchesFormUserId(long pathId, Long formUserId) {
        if (Objects.equals(pathId, formUserId)) return;

        throw new IllegalArgumentException(
                "Role change request userId mismatch: pathId=" + pathId
                        + ", formUserId=" + formUserId
        );
    }
}