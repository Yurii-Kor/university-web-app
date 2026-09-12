package ua.foxminded.university.web.course;

import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.persistence.EntityNotFoundException;
import ua.foxminded.university.model.persistence.teacher.projection.TeacherOptionView;
import ua.foxminded.university.service.course.CourseService;
import ua.foxminded.university.service.course.dto.CourseCreateDto;
import ua.foxminded.university.service.course.exception.CourseCreateException;
import ua.foxminded.university.service.teacher.TeacherService;
import ua.foxminded.university.web.testconfig.MethodSecurityTestConfig;
import ua.foxminded.university.web.util.ExceptionMessageReader;

@WebMvcTest(controllers = CourseCreateController.class)
@Import({
		CourseCreateExceptionHandler.class,
		MethodSecurityTestConfig.class,
		ExceptionMessageReader.class
})
class CourseCreateControllerWebMvcTest {

	private static final Long USER_ID = 42L;

	private static final String VALID_CODE = "SEC-303";
	private static final String VALID_NAME = "Security";
	private static final String VALID_DESC = "Intro";
	private static final String VALID_TEACHER_ID = "5";

	private static final CourseCreateDto VALID_FORM =
			new CourseCreateDto(
					VALID_CODE,
					VALID_NAME,
					VALID_DESC,
					5L);

	@Autowired
	MockMvc mockMvc;

	@MockitoBean
	CourseService courseService;

	@MockitoBean
	TeacherService teacherService;

	@Test
	@DisplayName("GET /courses/create as ADMIN -> 200, courses/create, model has form + teachers")
	void getCreateCourse_admin_ok_returnsCreateViewAndModel()
			throws Exception {

		var teachers =
				List.of(
						mock(
								TeacherOptionView.class));

		when(teacherService.listTeacherOptions())
				.thenReturn(
						teachers);

		mockMvc.perform(
				get("/courses/create")
						.with(user(
								USER_ID.toString())
								.roles("ADMIN")))
				.andExpect(
						status().isOk())
				.andExpect(
						view().name(
								"courses/create"))
				.andExpect(
						model().attributeExists(
								"form"))
				.andExpect(
						model().attribute(
								"all_teachers",
								sameInstance(
										teachers)));
	}

	@Test
	@DisplayName("GET /courses/create as TEACHER -> 302 redirect (forbidden in this app), controller not invoked")
	void getCreateCourse_teacher_forbidden()
			throws Exception {

		mockMvc.perform(
				get("/courses/create")
						.with(user(
								USER_ID.toString())
								.roles("TEACHER")))
				.andExpect(
						status().is3xxRedirection());

		verifyNoInteractions(
				courseService);
	}

	@Test
	@DisplayName("POST /courses/create as ADMIN ok -> redirects /courses, flash ok, calls service.create(dto)")
	void postCreateCourse_admin_ok_redirectsAndCallsService()
			throws Exception {

		mockMvc.perform(
				post("/courses/create")
						.with(user(
								USER_ID.toString())
								.roles("ADMIN"))
						.with(csrf())
						.param(
								"code",
								VALID_CODE)
						.param(
								"name",
								VALID_NAME)
						.param(
								"description",
								VALID_DESC)
						.param(
								"teacherId",
								VALID_TEACHER_ID))
				.andExpect(
						status().is3xxRedirection())
				.andExpect(
						redirectedUrl(
								"/courses"))
				.andExpect(
						flash().attribute(
								"ok",
								"Course created."));

		verify(courseService)
				.create(
						VALID_FORM);
	}

	@Test
	@DisplayName("POST /courses/create as ADMIN when @Valid fails -> 200, courses/create, field error, service not called")
	void postCreateCourse_validationViolation_staysOnFormAndNoServiceCall()
			throws Exception {

		var teachers =
				List.of(
						mock(
								TeacherOptionView.class));

		when(teacherService.listTeacherOptions())
				.thenReturn(
						teachers);

		mockMvc.perform(
				post("/courses/create")
						.with(user(
								USER_ID.toString())
								.roles("ADMIN"))
						.with(csrf())
						.param(
								"code",
								"")
						.param(
								"name",
								VALID_NAME)
						.param(
								"description",
								VALID_DESC)
						.param(
								"teacherId",
								VALID_TEACHER_ID))
				.andExpect(
						status().isOk())
				.andExpect(
						view().name(
								"courses/create"))
				.andExpect(
						model().attributeHasFieldErrors(
								"form",
								"code"))
				.andExpect(
						model().attributeExists(
								"all_teachers"));

		verify(
				courseService,
				never())
				.create(
						any());
	}

	@Test
	@DisplayName("POST /courses/create as TEACHER -> 302 redirect (forbidden), service not called")
	void postCreateCourse_teacher_forbidden()
			throws Exception {

		mockMvc.perform(
				post("/courses/create")
						.with(user(
								USER_ID.toString())
								.roles("TEACHER"))
						.with(csrf())
						.param(
								"code",
								VALID_CODE)
						.param(
								"name",
								VALID_NAME)
						.param(
								"description",
								VALID_DESC)
						.param(
								"teacherId",
								VALID_TEACHER_ID))
				.andExpect(
						status().is3xxRedirection());

		verifyNoInteractions(
				courseService);
	}

	@Test
	@DisplayName("POST /courses/create when service throws CourseCreateException -> returns create view with err and submitted form")
	void postCreateCourse_courseCreateException_returnsViewAndRestoresForm()
			throws Exception {

		var exception =
				new CourseCreateException(
						VALID_FORM,
						"Course name already exists: Security");

		doThrow(
				exception)
				.when(courseService)
				.create(
						any());

		mockMvc.perform(
				post("/courses/create")
						.with(user(
								USER_ID.toString())
								.roles("ADMIN"))
						.with(csrf())
						.param(
								"code",
								VALID_CODE)
						.param(
								"name",
								VALID_NAME)
						.param(
								"description",
								VALID_DESC)
						.param(
								"teacherId",
								VALID_TEACHER_ID))
				.andExpect(
						status().isOk())
				.andExpect(
						view().name(
								"courses/create"))
				.andExpect(
						model().attribute(
								"err",
								"Course name already exists: Security"))
				.andExpect(
						model().attribute(
								"form",
								VALID_FORM));
	}

	@Test
	@DisplayName("POST /courses/create when service throws IllegalArgumentException -> redirects /courses/create, flash err + empty form")
	void postCreateCourse_illegalArgument_redirectsCreateAndSetsErrAndForm()
			throws Exception {

		doThrow(
				new IllegalArgumentException(
						"boom"))
				.when(courseService)
				.create(
						any());

		mockMvc.perform(
				post("/courses/create")
						.with(user(
								USER_ID.toString())
								.roles("ADMIN"))
						.with(csrf())
						.param(
								"code",
								VALID_CODE)
						.param(
								"name",
								VALID_NAME)
						.param(
								"description",
								VALID_DESC)
						.param(
								"teacherId",
								VALID_TEACHER_ID))
				.andExpect(
						status().is3xxRedirection())
				.andExpect(
						redirectedUrl(
								"/courses/create"))
				.andExpect(
						flash().attribute(
								"err",
								"boom"))
				.andExpect(
						flash().attribute(
								"form",
								new CourseCreateDto(
										null,
										null,
										null,
										null)));
	}

	@Test
	@DisplayName("POST /courses/create when service throws EntityNotFoundException -> redirects /courses/create, flash err + empty form")
	void postCreateCourse_entityNotFound_redirectsCreateAndSetsErrAndForm()
			throws Exception {

		doThrow(
				new EntityNotFoundException(
						"Teacher not found: id=5"))
				.when(courseService)
				.create(
						any());

		mockMvc.perform(
				post("/courses/create")
						.with(user(
								USER_ID.toString())
								.roles("ADMIN"))
						.with(csrf())
						.param(
								"code",
								VALID_CODE)
						.param(
								"name",
								VALID_NAME)
						.param(
								"description",
								VALID_DESC)
						.param(
								"teacherId",
								VALID_TEACHER_ID))
				.andExpect(
						status().is3xxRedirection())
				.andExpect(
						redirectedUrl(
								"/courses/create"))
				.andExpect(
						flash().attribute(
								"err",
								"Teacher not found: id=5"))
				.andExpect(
						flash().attribute(
								"form",
								new CourseCreateDto(
										null,
										null,
										null,
										null)));
	}

	@Test
	@DisplayName("POST /courses/create when service throws DataIntegrityViolationException -> redirects /courses/create, flash fixed err + empty form")
	void postCreateCourse_dbConflict_redirectsCreateAndSetsFixedErrAndForm()
			throws Exception {

		doThrow(
				new DataIntegrityViolationException(
						"conflict"))
				.when(courseService)
				.create(
						any());

		mockMvc.perform(
				post("/courses/create")
						.with(user(
								USER_ID.toString())
								.roles("ADMIN"))
						.with(csrf())
						.param(
								"code",
								VALID_CODE)
						.param(
								"name",
								VALID_NAME)
						.param(
								"description",
								VALID_DESC)
						.param(
								"teacherId",
								VALID_TEACHER_ID))
				.andExpect(
						status().is3xxRedirection())
				.andExpect(
						redirectedUrl(
								"/courses/create"))
				.andExpect(
						flash().attribute(
								"err",
								"Course code or name already exists."))
				.andExpect(
						flash().attribute(
								"form",
								new CourseCreateDto(
										null,
										null,
										null,
										null)));
	}

	@Test
	@DisplayName("POST /courses/create when service throws RuntimeException -> redirects /courses/create, flash generic err + empty form")
	void postCreateCourse_runtimeException_redirectsCreateAndSetsGenericErrAndForm()
			throws Exception {

		doThrow(
				new RuntimeException(
						"boom"))
				.when(courseService)
				.create(
						any());

		mockMvc.perform(
				post("/courses/create")
						.with(user(
								USER_ID.toString())
								.roles("ADMIN"))
						.with(csrf())
						.param(
								"code",
								VALID_CODE)
						.param(
								"name",
								VALID_NAME)
						.param(
								"description",
								VALID_DESC)
						.param(
								"teacherId",
								VALID_TEACHER_ID))
				.andExpect(
						status().is3xxRedirection())
				.andExpect(
						redirectedUrl(
								"/courses/create"))
				.andExpect(
						flash().attribute(
								"err",
								"Something went wrong."))
				.andExpect(
						flash().attribute(
								"form",
								new CourseCreateDto(
										null,
										null,
										null,
										null)));
	}
}