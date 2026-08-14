package ua.foxminded.university.web.group.details;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import ua.foxminded.university.service.studygroup.StudyGroupService;
import ua.foxminded.university.web.util.PrincipalHandler;

@Component
@RequiredArgsConstructor
public class GroupDetailsPageModelFactory {

	private static final String PAGE_TITLE = "My group";
	private static final String PAGE_SUBTITLE = "Your study group.";

	private final StudyGroupService groupService;
	private final PrincipalHandler principalHandler;

	public StudentGroupPageModel build(
			UserDetails principal) {

		var studentId = principalHandler.parseUserId(
				principal);

		var group = groupService.getGroupHeaderForStudent(
				studentId);

		return new StudentGroupPageModel(
				PAGE_TITLE,
				PAGE_SUBTITLE,
				group.id(),
				group.name(),
				group.studentCount());
	}
}