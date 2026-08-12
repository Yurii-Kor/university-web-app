package ua.foxminded.university.web.group;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import ua.foxminded.university.service.studygroup.StudyGroupService;
import ua.foxminded.university.service.studygroup.dto.StudyGroupRenameDto;
import ua.foxminded.university.service.studygroup.exception.StudyGroupRenameException;
import ua.foxminded.university.service.teacher.TeacherService;
import ua.foxminded.university.service.util.validation.ContextConstraintViolationException;
import ua.foxminded.university.web.group.cards.GroupsCardsPageModelFactory;
import ua.foxminded.university.web.testconfig.MethodSecurityTestConfig;
import ua.foxminded.university.web.util.ExceptionMessageReader;
import ua.foxminded.university.web.util.PrincipalHandler;

@WebMvcTest(controllers = GroupsManagementController.class)
@Import({
	GroupsManagementExceptionHandler.class,
	MethodSecurityTestConfig.class,
	ExceptionMessageReader.class
})
class GroupsManagementRenameWebMvcTest {

	private static final long ADMIN_ID = 41L;
	private static final long TEACHER_ID = 42L;
	private static final long STUDENT_ID = 43L;

	private static final long GROUP_ID = 1001L;

	private static final String NEW_NAME = "CS-202";
	private static final String DUPLICATE_NAME = "CS-101";
	private static final String INVALID_NAME = "bad!";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private StudyGroupService groupService;

	@MockitoBean
	private GroupsCardsPageModelFactory pageFactory;

	@MockitoBean
	private TeacherService teacherService;

	@MockitoBean
	private PrincipalHandler principalHandler;

	@Test
	@DisplayName("POST /groups/{id}/rename as admin -> redirects to same page, flash ok/groupId, calls service")
	void renameGroup_admin_ok()
			throws Exception {

		mockMvc.perform(
				post("/groups/{id}/rename", GROUP_ID)
						.with(user(
								Long.toString(ADMIN_ID))
								.roles("ADMIN"))
						.with(csrf())
						.param("name", NEW_NAME)
						.param("page", "2"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl(
						"/groups?page=2"))
				.andExpect(flash().attribute(
						"ok",
						"Group renamed."))
				.andExpect(flash().attribute(
						"groupId",
						GROUP_ID));

		verify(groupService)
				.rename(new StudyGroupRenameDto(
						GROUP_ID,
						NEW_NAME));
	}

	@Test
	@DisplayName("POST /groups/{id}/rename when name exists -> redirects, flash err/groupId/requestedName")
	void renameGroup_duplicateName_redirectsWithCardError()
			throws Exception {

		var message = "Study group name already exists.";

		var ex = new StudyGroupRenameException(
				GROUP_ID,
				DUPLICATE_NAME,
				message);

		when(groupService.rename(any()))
				.thenThrow(ex);

		mockMvc.perform(
				post("/groups/{id}/rename", GROUP_ID)
						.with(user(
								Long.toString(ADMIN_ID))
								.roles("ADMIN"))
						.with(csrf())
						.param("name", DUPLICATE_NAME)
						.param("page", "0"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl(
						"/groups"))
				.andExpect(flash().attribute(
						"err",
						message))
				.andExpect(flash().attribute(
						"groupId",
						GROUP_ID))
				.andExpect(flash().attribute(
						"requestedName",
						DUPLICATE_NAME));

		verify(groupService)
				.rename(new StudyGroupRenameDto(
						GROUP_ID,
						DUPLICATE_NAME));
	}

	@Test
	@DisplayName("POST /groups/{id}/rename when validation fails -> redirects, flash err/groupId")
	void renameGroup_invalidName_redirectsWithCardError()
			throws Exception {

		@SuppressWarnings("unchecked")
		ConstraintViolation<Object> violation =
				org.mockito.Mockito.mock(
						ConstraintViolation.class);

		Path path = org.mockito.Mockito.mock(
				Path.class);

		when(path.toString())
				.thenReturn("name");

		when(violation.getPropertyPath())
				.thenReturn(path);

		when(violation.getMessage())
				.thenReturn("invalid");

		var ex = new ContextConstraintViolationException(
				GROUP_ID,
				Set.of(violation));

		when(groupService.rename(any()))
				.thenThrow(ex);

		mockMvc.perform(
				post("/groups/{id}/rename", GROUP_ID)
						.with(user(
								Long.toString(ADMIN_ID))
								.roles("ADMIN"))
						.with(csrf())
						.param("name", INVALID_NAME)
						.param("page", "0"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl(
						"/groups"))
				.andExpect(flash().attribute(
						"err",
						"name: invalid"))
				.andExpect(flash().attribute(
						"groupId",
						GROUP_ID));

		verify(groupService)
				.rename(new StudyGroupRenameDto(
						GROUP_ID,
						INVALID_NAME));
	}

	@Test
	@DisplayName("POST /groups/{id}/rename when group not found -> redirects, flash err")
	void renameGroup_notFound_redirectsWithError()
			throws Exception {

		var message =
				"Study group not found: id=" + GROUP_ID;

		when(groupService.rename(any()))
				.thenThrow(new EntityNotFoundException(
						message));

		mockMvc.perform(
				post("/groups/{id}/rename", GROUP_ID)
						.with(user(
								Long.toString(ADMIN_ID))
								.roles("ADMIN"))
						.with(csrf())
						.param("name", NEW_NAME)
						.param("page", "0"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl(
						"/groups"))
				.andExpect(flash().attribute(
						"err",
						message));

		verify(groupService)
				.rename(new StudyGroupRenameDto(
						GROUP_ID,
						NEW_NAME));
	}

	@Test
	@DisplayName("POST /groups/{id}/rename as teacher -> 403, service not called")
	void renameGroup_teacher_forbidden()
	        throws Exception {

	    mockMvc.perform(
	            post("/groups/{id}/rename", GROUP_ID)
	                    .with(user(
	                            Long.toString(TEACHER_ID))
	                            .roles("TEACHER"))
	                    .with(csrf())
	                    .param("name", NEW_NAME))
	            .andExpect(status().isForbidden());

	    verify(groupService, never())
	            .rename(any());
	}

	@Test
	@DisplayName("POST /groups/{id}/rename as student -> 403, service not called")
	void renameGroup_student_forbidden()
	        throws Exception {

	    mockMvc.perform(
	            post("/groups/{id}/rename", GROUP_ID)
	                    .with(user(
	                            Long.toString(STUDENT_ID))
	                            .roles("STUDENT"))
	                    .with(csrf())
	                    .param("name", NEW_NAME))
	            .andExpect(status().isForbidden());

	    verify(groupService, never())
	            .rename(any());
	}

	@Test
	@DisplayName("POST /groups/{id}/rename unauthenticated -> 401, service not called")
	void renameGroup_unauthenticated_unauthorized()
			throws Exception {

		mockMvc.perform(
				post("/groups/{id}/rename", GROUP_ID)
						.with(csrf())
						.param("name", NEW_NAME))
				.andExpect(status().isUnauthorized());

		verify(groupService, never())
				.rename(any());
	}
}