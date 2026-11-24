package ua.foxminded.university.web.teacher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static ua.foxminded.university.web.teacher.TeachersManagementWebMvcTestConstants.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.BindingResult;

import ua.foxminded.university.service.teacher.TeacherService;
import ua.foxminded.university.service.teacher.dto.TeacherCreateDto;
import ua.foxminded.university.service.teacher.exception.TeacherCreateException;
import ua.foxminded.university.web.testconfig.MethodSecurityTestConfig;
import ua.foxminded.university.web.util.ExceptionMessageReader;

@WebMvcTest(controllers = TeacherCreateController.class)
@Import({
		TeacherCreateExceptionHandler.class,
		MethodSecurityTestConfig.class,
		ExceptionMessageReader.class
})
class TeacherCreateControllerWebMvcTest {

	private static final String EMAIL_CONFLICT_MESSAGE =
			"An account with this email already exists.";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private TeacherService teacherService;

	@Test
	@DisplayName("GET /teachers/create as admin -> 200, teachers/create, model contains form")
	void getCreateTeacher_admin_ok()
			throws Exception {

		var result = mockMvc.perform(
				get("/teachers/create")
						.with(user(
								Long.toString(ADMIN_ID))
								.roles("ADMIN")))
				.andExpect(status().isOk())
				.andExpect(view().name(
						"teachers/create"))
				.andExpect(model().attributeExists(
						"form"))
				.andReturn();

		var form = result.getModelAndView()
				.getModel()
				.get("form");

		assertInstanceOf(
				TeacherCreateDto.class,
				form);
	}

	@Test
	@DisplayName("POST /teachers/create as admin -> creates teacher and redirects to teachers")
	void createTeacher_admin_ok()
			throws Exception {

		var expected = createDto();

		mockMvc.perform(
				post("/teachers/create")
						.with(user(
								Long.toString(ADMIN_ID))
								.roles("ADMIN"))
						.with(csrf())
						.param(
								"email",
								TEACHER_EMAIL)
						.param(
								"password",
								TEACHER_PASSWORD)
						.param(
								"firstName",
								TEACHER_FIRST_NAME)
						.param(
								"lastName",
								TEACHER_LAST_NAME)
						.param(
								"academicRank",
								TEACHER_RANK.name())
						.param(
								"office",
								TEACHER_OFFICE))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl(
						"/teachers"))
				.andExpect(flash().attribute(
						"ok",
						"Teacher created."));

		verify(teacherService)
				.create(expected);
	}

	@Test
	@DisplayName("POST /teachers/create invalid form -> 200, field error, service not called")
	void createTeacher_invalidForm_returnsCreateView()
			throws Exception {

		mockMvc.perform(
				post("/teachers/create")
						.with(user(
								Long.toString(ADMIN_ID))
								.roles("ADMIN"))
						.with(csrf())
						.param(
								"email",
								"")
						.param(
								"password",
								TEACHER_PASSWORD)
						.param(
								"firstName",
								TEACHER_FIRST_NAME)
						.param(
								"lastName",
								TEACHER_LAST_NAME)
						.param(
								"academicRank",
								TEACHER_RANK.name())
						.param(
								"office",
								TEACHER_OFFICE))
				.andExpect(status().isOk())
				.andExpect(view().name(
						"teachers/create"))
				.andExpect(model().attributeHasFieldErrors(
						"form",
						"email"));

		verify(
				teacherService,
				never())
				.create(
						org.mockito.ArgumentMatchers.any());
	}

	@Test
	@DisplayName("POST /teachers/create duplicate email -> keeps form and adds email field error")
	void createTeacher_emailConflict_keepsFormAndAddsFieldError()
			throws Exception {

		var submitted = createDto();

		when(teacherService.create(
				submitted))
				.thenThrow(
						new TeacherCreateException(
								"email",
								submitted,
								EMAIL_CONFLICT_MESSAGE));

		var result = mockMvc.perform(
				post("/teachers/create")
						.with(user(
								Long.toString(ADMIN_ID))
								.roles("ADMIN"))
						.with(csrf())
						.param(
								"email",
								TEACHER_EMAIL)
						.param(
								"password",
								TEACHER_PASSWORD)
						.param(
								"firstName",
								TEACHER_FIRST_NAME)
						.param(
								"lastName",
								TEACHER_LAST_NAME)
						.param(
								"academicRank",
								TEACHER_RANK.name())
						.param(
								"office",
								TEACHER_OFFICE))
				.andExpect(status().isOk())
				.andExpect(view().name(
						"teachers/create"))
				.andExpect(model().attribute(
						"form",
						submitted))
				.andExpect(model().attributeHasFieldErrors(
						"form",
						"email"))
				.andReturn();

		assertFieldError(
				result.getModelAndView()
						.getModel()
						.get(
								BindingResult.MODEL_KEY_PREFIX
										+ "form"),
				"email",
				EMAIL_CONFLICT_MESSAGE);

		verify(teacherService)
				.create(submitted);
	}

	@Test
	@DisplayName("GET /teachers/create as teacher -> 403")
	void getCreateTeacher_teacher_forbidden()
			throws Exception {

		mockMvc.perform(
				get("/teachers/create")
						.with(user(
								Long.toString(
										TEACHER_USER_ID))
								.roles("TEACHER")))
				.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("GET /teachers/create as student -> 403")
	void getCreateTeacher_student_forbidden()
			throws Exception {

		mockMvc.perform(
				get("/teachers/create")
						.with(user(
								Long.toString(
										STUDENT_USER_ID))
								.roles("STUDENT")))
				.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("GET /teachers/create unauthenticated -> 401")
	void getCreateTeacher_unauthenticated_unauthorized()
			throws Exception {

		mockMvc.perform(
				get("/teachers/create"))
				.andExpect(status().isUnauthorized());
	}

	private TeacherCreateDto createDto() {

		return new TeacherCreateDto(
				TEACHER_EMAIL,
				TEACHER_PASSWORD,
				TEACHER_FIRST_NAME,
				TEACHER_LAST_NAME,
				TEACHER_RANK,
				TEACHER_OFFICE);
	}

	private void assertFieldError(
			Object value,
			String field,
			String expectedMessage) {

		var bindingResult = assertInstanceOf(
				BindingResult.class,
				value);

		var fieldError = bindingResult.getFieldError(
				field);

		assertNotNull(
				fieldError);

		assertEquals(
				expectedMessage,
				fieldError.getDefaultMessage());

		assertNull(
				fieldError.getRejectedValue());
	}
}