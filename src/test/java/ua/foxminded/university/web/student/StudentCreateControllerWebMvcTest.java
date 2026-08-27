package ua.foxminded.university.web.student;

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
import static ua.foxminded.university.web.student.StudentsManagementWebMvcTestConstants.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.BindingResult;

import ua.foxminded.university.model.persistence.studygroup.projection.GroupView;
import ua.foxminded.university.service.student.StudentService;
import ua.foxminded.university.service.student.dto.StudentCreateDto;
import ua.foxminded.university.service.student.exception.StudentCreateException;
import ua.foxminded.university.service.studygroup.StudyGroupService;
import ua.foxminded.university.service.teacher.TeacherService;
import ua.foxminded.university.web.testconfig.MethodSecurityTestConfig;
import ua.foxminded.university.web.util.ExceptionMessageReader;

@WebMvcTest(controllers = StudentCreateController.class)
@Import({
		StudentCreateExceptionHandler.class,
		MethodSecurityTestConfig.class,
		ExceptionMessageReader.class
})
class StudentCreateControllerWebMvcTest {

	private static final String EMAIL_CONFLICT_MESSAGE =
			"An account with this email already exists.";

	private static final String GROUP_MISSING_MESSAGE =
			"Selected study group is no longer available.";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private StudentService studentService;

	@MockitoBean
	private StudyGroupService groupService;

	@MockitoBean
	private TeacherService teacherService;

	@Test
	@DisplayName("GET /students/create as admin -> 200, students/create, model contains form and groups")
	void getCreateStudent_admin_ok()
			throws Exception {

		var groups = groupOptions();

		when(groupService.listGroupOptions())
				.thenReturn(groups);

		var result = mockMvc.perform(
				get("/students/create")
						.with(user(
								Long.toString(ADMIN_ID))
								.roles("ADMIN")))
				.andExpect(status().isOk())
				.andExpect(view().name(
						"students/create"))
				.andExpect(model().attributeExists(
						"form"))
				.andExpect(model().attribute(
						"all_groups",
						groups))
				.andReturn();

		var form = result.getModelAndView()
				.getModel()
				.get("form");

		assertInstanceOf(
				StudentCreateDto.class,
				form);

		verify(groupService)
				.listGroupOptions();
	}

	@Test
	@DisplayName("POST /students/create as admin -> creates student and redirects to students")
	void createStudent_admin_ok()
			throws Exception {

		var expected = createDto(
				STUDENT_ENROLLMENT_YEAR);

		mockMvc.perform(
				post("/students/create")
						.with(user(
								Long.toString(ADMIN_ID))
								.roles("ADMIN"))
						.with(csrf())
						.param(
								"email",
								STUDENT_EMAIL)
						.param(
								"password",
								STUDENT_PASSWORD)
						.param(
								"firstName",
								STUDENT_FIRST_NAME)
						.param(
								"lastName",
								STUDENT_LAST_NAME)
						.param(
								"groupId",
								Long.toString(GROUP_CS_ID))
						.param(
								"enrollmentYear",
								Integer.toString(STUDENT_ENROLLMENT_YEAR)))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl(
						"/students"))
				.andExpect(flash().attribute(
						"ok",
						"Student created."));

		verify(studentService)
				.create(expected);
	}

	@Test
	@DisplayName("POST /students/create without enrollment year -> creates student with null year")
	void createStudent_withoutEnrollmentYear_ok()
			throws Exception {

		var expected = createDto(null);

		mockMvc.perform(
				post("/students/create")
						.with(user(
								Long.toString(ADMIN_ID))
								.roles("ADMIN"))
						.with(csrf())
						.param(
								"email",
								STUDENT_EMAIL)
						.param(
								"password",
								STUDENT_PASSWORD)
						.param(
								"firstName",
								STUDENT_FIRST_NAME)
						.param(
								"lastName",
								STUDENT_LAST_NAME)
						.param(
								"groupId",
								Long.toString(GROUP_CS_ID)))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl(
						"/students"))
				.andExpect(flash().attribute(
						"ok",
						"Student created."));

		verify(studentService)
				.create(expected);
	}

	@Test
	@DisplayName("POST /students/create invalid form -> 200, field error, service not called")
	void createStudent_invalidForm_returnsCreateView()
			throws Exception {

		var groups = groupOptions();

		when(groupService.listGroupOptions())
				.thenReturn(groups);

		mockMvc.perform(
				post("/students/create")
						.with(user(
								Long.toString(ADMIN_ID))
								.roles("ADMIN"))
						.with(csrf())
						.param(
								"email",
								"")
						.param(
								"password",
								STUDENT_PASSWORD)
						.param(
								"firstName",
								STUDENT_FIRST_NAME)
						.param(
								"lastName",
								STUDENT_LAST_NAME)
						.param(
								"groupId",
								Long.toString(GROUP_CS_ID))
						.param(
								"enrollmentYear",
								Integer.toString(STUDENT_ENROLLMENT_YEAR)))
				.andExpect(status().isOk())
				.andExpect(view().name(
						"students/create"))
				.andExpect(model().attributeHasFieldErrors(
						"form",
						"email"))
				.andExpect(model().attribute(
						"all_groups",
						groups));

		verify(
				studentService,
				never())
				.create(
						org.mockito.ArgumentMatchers.any());

		verify(groupService)
				.listGroupOptions();
	}

	@Test
	@DisplayName("POST /students/create duplicate email -> keeps form and clears rejected email")
	void createStudent_emailConflict_keepsFormAndAddsFieldError()
			throws Exception {

		var submitted = createDto(
				STUDENT_ENROLLMENT_YEAR);

		var groups = groupOptions();

		when(studentService.create(submitted))
				.thenThrow(new StudentCreateException(
						"email",
						submitted,
						EMAIL_CONFLICT_MESSAGE));

		when(groupService.listGroupOptions())
				.thenReturn(groups);

		var result = mockMvc.perform(
				post("/students/create")
						.with(user(
								Long.toString(ADMIN_ID))
								.roles("ADMIN"))
						.with(csrf())
						.param(
								"email",
								STUDENT_EMAIL)
						.param(
								"password",
								STUDENT_PASSWORD)
						.param(
								"firstName",
								STUDENT_FIRST_NAME)
						.param(
								"lastName",
								STUDENT_LAST_NAME)
						.param(
								"groupId",
								Long.toString(GROUP_CS_ID))
						.param(
								"enrollmentYear",
								Integer.toString(STUDENT_ENROLLMENT_YEAR)))
				.andExpect(status().isOk())
				.andExpect(view().name(
						"students/create"))
				.andExpect(model().attribute(
						"form",
						submitted))
				.andExpect(model().attributeHasFieldErrors(
						"form",
						"email"))
				.andExpect(model().attribute(
						"all_groups",
						groups))
				.andReturn();

		assertFieldError(
				result.getModelAndView()
						.getModel()
						.get(BindingResult.MODEL_KEY_PREFIX + "form"),
				"email",
				EMAIL_CONFLICT_MESSAGE);

		verify(studentService)
				.create(submitted);

		verify(groupService)
				.listGroupOptions();
	}

	@Test
	@DisplayName("POST /students/create missing group -> keeps form and clears rejected group")
	void createStudent_missingGroup_keepsFormAndAddsFieldError()
			throws Exception {

		var submitted = createDto(
				STUDENT_ENROLLMENT_YEAR);

		var groups = groupOptions();

		when(studentService.create(submitted))
				.thenThrow(new StudentCreateException(
						"groupId",
						submitted,
						GROUP_MISSING_MESSAGE));

		when(groupService.listGroupOptions())
				.thenReturn(groups);

		var result = mockMvc.perform(
				post("/students/create")
						.with(user(
								Long.toString(ADMIN_ID))
								.roles("ADMIN"))
						.with(csrf())
						.param(
								"email",
								STUDENT_EMAIL)
						.param(
								"password",
								STUDENT_PASSWORD)
						.param(
								"firstName",
								STUDENT_FIRST_NAME)
						.param(
								"lastName",
								STUDENT_LAST_NAME)
						.param(
								"groupId",
								Long.toString(GROUP_CS_ID))
						.param(
								"enrollmentYear",
								Integer.toString(STUDENT_ENROLLMENT_YEAR)))
				.andExpect(status().isOk())
				.andExpect(view().name(
						"students/create"))
				.andExpect(model().attribute(
						"form",
						submitted))
				.andExpect(model().attributeHasFieldErrors(
						"form",
						"groupId"))
				.andExpect(model().attribute(
						"all_groups",
						groups))
				.andReturn();

		assertFieldError(
				result.getModelAndView()
						.getModel()
						.get(BindingResult.MODEL_KEY_PREFIX + "form"),
				"groupId",
				GROUP_MISSING_MESSAGE);

		verify(studentService)
				.create(submitted);

		verify(groupService)
				.listGroupOptions();
	}

	@Test
	@DisplayName("GET /students/create as teacher -> 403")
	void getCreateStudent_teacher_forbidden()
			throws Exception {

		mockMvc.perform(
				get("/students/create")
						.with(user(
								Long.toString(TEACHER_ID))
								.roles("TEACHER")))
				.andExpect(status().isForbidden());

		verify(
				groupService,
				never())
				.listGroupOptions();
	}

	@Test
	@DisplayName("GET /students/create as student -> 403")
	void getCreateStudent_student_forbidden()
			throws Exception {

		mockMvc.perform(
				get("/students/create")
						.with(user(
								Long.toString(STUDENT_USER_ID))
								.roles("STUDENT")))
				.andExpect(status().isForbidden());

		verify(
				groupService,
				never())
				.listGroupOptions();
	}

	@Test
	@DisplayName("GET /students/create unauthenticated -> 401")
	void getCreateStudent_unauthenticated_unauthorized()
			throws Exception {

		mockMvc.perform(
				get("/students/create"))
				.andExpect(status().isUnauthorized());

		verify(
				groupService,
				never())
				.listGroupOptions();
	}

	private StudentCreateDto createDto(
			Integer enrollmentYear) {

		return new StudentCreateDto(
				STUDENT_EMAIL,
				STUDENT_PASSWORD,
				STUDENT_FIRST_NAME,
				STUDENT_LAST_NAME,
				GROUP_CS_ID,
				enrollmentYear);
	}

	private List<GroupView> groupOptions() {

		return List.of(
				new GroupView(
						GROUP_CS_ID,
						GROUP_CS),
				new GroupView(
						GROUP_SE_ID,
						GROUP_SE));
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

		assertNotNull(fieldError);

		assertEquals(
				expectedMessage,
				fieldError.getDefaultMessage());

		assertNull(
				fieldError.getRejectedValue());
	}
}