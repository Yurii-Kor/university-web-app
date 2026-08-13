package ua.foxminded.university.web.group;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import ua.foxminded.university.service.studygroup.StudyGroupService;
import ua.foxminded.university.service.studygroup.dto.StudyGroupCreateDto;
import ua.foxminded.university.service.teacher.TeacherService;
import ua.foxminded.university.web.testconfig.MethodSecurityTestConfig;
import ua.foxminded.university.web.util.ExceptionMessageReader;

@WebMvcTest(controllers = GroupCreateController.class)
@Import({
		GroupCreateExceptionHandler.class,
		MethodSecurityTestConfig.class,
		ExceptionMessageReader.class
})
class GroupCreatePostWebMvcTest {

	private static final long ADMIN_ID = 41L;
	private static final long TEACHER_ID = 42L;
	private static final long STUDENT_ID = 43L;

	private static final String VALID_NAME = "SE-205";
	private static final String INVALID_NAME = "wrong";
	private static final String EXISTING_NAME = "CS-101";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private StudyGroupService groupService;

	@MockitoBean
	private TeacherService teacherService;

	@Test
	@DisplayName("POST /groups/create as admin with valid form -> creates group and redirects")
	void createGroup_admin_valid_success()
			throws Exception {

		mockMvc.perform(
				post("/groups/create")
						.with(user(
								Long.toString(ADMIN_ID))
								.roles("ADMIN"))
						.with(csrf())
						.param("name", VALID_NAME))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl(
						"/groups"))
				.andExpect(flash().attribute(
						"ok",
						"Group created."));

		verify(groupService)
				.create(new StudyGroupCreateDto(
						VALID_NAME));
	}

	@Test
	@DisplayName("POST /groups/create as admin with invalid form -> validation error, service not called")
	void createGroup_admin_invalid_validationFails()
			throws Exception {

		mockMvc.perform(
				post("/groups/create")
						.with(user(
								Long.toString(ADMIN_ID))
								.roles("ADMIN"))
						.with(csrf())
						.param("name", INVALID_NAME))
				.andExpect(status().isOk())
				.andExpect(view().name(
						"groups/create"))
				.andExpect(model().attributeHasFieldErrors(
						"form",
						"name"));

		verify(groupService, never())
				.create(any());
	}

	@Test
	@DisplayName("POST /groups/create with existing name -> redirects to create page with error")
	void createGroup_duplicateName_redirectsWithError()
			throws Exception {

		var form = new StudyGroupCreateDto(
				EXISTING_NAME);

		var message =
				"Study group name already exists: "
						+ EXISTING_NAME;

		when(groupService.create(form))
				.thenThrow(
						new IllegalArgumentException(
								message));

		mockMvc.perform(
				post("/groups/create")
						.with(user(
								Long.toString(ADMIN_ID))
								.roles("ADMIN"))
						.with(csrf())
						.param("name", EXISTING_NAME))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl(
						"/groups/create"))
				.andExpect(flash().attribute(
						"err",
						message));

		verify(groupService)
				.create(form);
	}

	@Test
	@DisplayName("POST /groups/create as teacher -> 403, service not called")
	void createGroup_teacher_forbidden()
			throws Exception {

		mockMvc.perform(
				post("/groups/create")
						.with(user(
								Long.toString(TEACHER_ID))
								.roles("TEACHER"))
						.with(csrf())
						.param("name", VALID_NAME))
				.andExpect(status().isForbidden());

		verify(groupService, never())
				.create(any());
	}

	@Test
	@DisplayName("POST /groups/create as student -> 403, service not called")
	void createGroup_student_forbidden()
			throws Exception {

		mockMvc.perform(
				post("/groups/create")
						.with(user(
								Long.toString(STUDENT_ID))
								.roles("STUDENT"))
						.with(csrf())
						.param("name", VALID_NAME))
				.andExpect(status().isForbidden());

		verify(groupService, never())
				.create(any());
	}

	@Test
	@DisplayName("POST /groups/create unauthenticated -> 401, service not called")
	void createGroup_unauthenticated_unauthorized()
			throws Exception {

		mockMvc.perform(
				post("/groups/create")
						.with(csrf())
						.param("name", VALID_NAME))
				.andExpect(status().isUnauthorized());

		verify(groupService, never())
				.create(any());
	}
}