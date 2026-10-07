package ua.foxminded.university.web.group;

import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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

		var page = pageFactory.build(
				principal,
				pageNumber);

		model.addAttribute(
				"page",
				page);

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
			@RequestParam("name") String name,
			@RequestParam(name = "page", defaultValue = "0") int pageNumber,
			RedirectAttributes ra) {

		var patch = new StudyGroupRenameDto(
				id,
				name);

		groupService.rename(patch);

		ra.addFlashAttribute(
				"ok",
				"Group renamed.");

		ra.addFlashAttribute(
				"groupId",
				id);

		return "redirect:/groups?page="
				+ normalizePage(pageNumber);
	}

	@PostMapping("/{id}/delete")
	@PreAuthorize("hasRole('ADMIN')")
	public String deleteGroup(
			@PathVariable("id") long id,
			@RequestParam(name = "page", defaultValue = "0") int pageNumber,
			RedirectAttributes ra) {

		groupService.delete(id);

		ra.addFlashAttribute(
				"ok",
				"Group deleted.");

		return "redirect:/groups?page="
				+ normalizePage(pageNumber);
	}

	private int normalizePage(
			int pageNumber) {

		return Math.max(
				pageNumber,
				0);
	}
}