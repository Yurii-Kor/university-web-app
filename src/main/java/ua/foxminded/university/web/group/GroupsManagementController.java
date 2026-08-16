package ua.foxminded.university.web.group;

import java.util.Optional;

import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ua.foxminded.university.service.studygroup.StudyGroupService;
import ua.foxminded.university.service.studygroup.dto.StudyGroupRenameDto;
import ua.foxminded.university.web.group.cards.GroupsCardsPageModelFactory;

@Controller
@RequestMapping("/groups")
@RequiredArgsConstructor
public class GroupsManagementController {

	private final StudyGroupService groupService;
	private final GroupsCardsPageModelFactory pageFactory;

	@GetMapping
	@PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
	public String groupsPage(
			@AuthenticationPrincipal UserDetails principal,
			@RequestParam(name = "page", defaultValue = "0") int pageNumber,
			Model model) {

        var page = pageFactory.build(principal, pageNumber);

        model.addAttribute("page", page);

		return "groups/groups";
	}

	@InitBinder
	void initBinder(WebDataBinder binder) {
		binder.registerCustomEditor(
				String.class,
				new StringTrimmerEditor(true));
	}

	@PostMapping("/{id}/rename")
	@PreAuthorize("hasRole('ADMIN')")
	public String renameGroup(
	        @PathVariable("id") long id,
	        @Valid @ModelAttribute("patch") StudyGroupRenameDto patch,
	        BindingResult br,
	        @RequestParam(name = "page", defaultValue = "0") int pageNumber,
	        RedirectAttributes ra) {

        if (br.hasErrors()) {
            addRenameValidationErrors(id, patch, br, ra);

            return redirectToGroups(pageNumber);
        }

	    groupService.rename(
	            id,
	            patch);

	    ra.addFlashAttribute(
	            "ok",
	            "Group renamed.");

	    ra.addFlashAttribute(
	            "groupId",
	            id);

	    return redirectToGroups(pageNumber);
	}

	@PostMapping("/{id}/delete")
	@PreAuthorize("hasRole('ADMIN')")
	public String deleteGroup(
			@PathVariable("id") long id,
			@RequestParam(name = "page", defaultValue = "0") int pageNumber,
			RedirectAttributes ra) {

		groupService.delete(id);

        ra.addFlashAttribute("ok", "Group deleted.");

		return redirectToGroups(pageNumber);
	}

    private String redirectToGroups(int pageNumber) {
        return "redirect:/groups?page=" + normalizePage(pageNumber);
    }

    private int normalizePage(int pageNumber) {
        return Math.max(pageNumber, 0);
    }
    
    private void addRenameValidationErrors(
            long id,
            StudyGroupRenameDto patch,
            BindingResult br,
            RedirectAttributes ra) {

        ra.addFlashAttribute(
                "groupId",
                id);

        ra.addFlashAttribute(
                "requestedName",
                Optional.ofNullable(patch.name())
                        .orElse(""));

        ra.addFlashAttribute(
                "err",
                Optional.ofNullable(br.getFieldError("name"))
                        .map(error -> error.getDefaultMessage())
                        .orElse("Invalid group name."));
    }
}