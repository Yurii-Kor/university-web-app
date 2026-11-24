package ua.foxminded.university.web.teacher;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ua.foxminded.university.web.teacher.TeachersManagementWebMvcTestConstants.*;

import jakarta.persistence.EntityNotFoundException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import ua.foxminded.university.service.teacher.TeacherService;
import ua.foxminded.university.service.teacher.dto.TeacherUpdateDto;
import ua.foxminded.university.web.teacher.page.TeachersPageModelFactory;
import ua.foxminded.university.web.testconfig.MethodSecurityTestConfig;
import ua.foxminded.university.web.util.ExceptionMessageReader;

@WebMvcTest(controllers = TeachersManagementController.class)
@Import({
		TeachersManagementExceptionHandler.class,
		MethodSecurityTestConfig.class,
		ExceptionMessageReader.class
})
class TeachersManagementUpdateWebMvcTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private TeacherService teacherService;

	@MockitoBean
	private TeachersPageModelFactory pageFactory;

	@Test
	@DisplayName("POST /teachers/{id}/update as admin -> redirects to same page, flash ok/teacherId, calls service")
	void updateTeacher_admin_ok()
			throws Exception {

		mockMvc.perform(
				post(
						"/teachers/{id}/update",
						TEACHER_ID)
						.with(user(
								Long.toString(ADMIN_ID))
								.roles("ADMIN"))
						.with(csrf())
						.param(
								"academicRank",
								UPDATED_RANK.name())
						.param(
								"office",
								UPDATED_OFFICE)
						.param(
								"page",
								"2"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl(
						"/teachers?page=2"))
				.andExpect(flash().attribute(
						"ok",
						"Teacher updated."))
				.andExpect(flash().attribute(
						"teacherId",
						TEACHER_ID));

		verify(teacherService)
				.update(
						TEACHER_ID,
						new TeacherUpdateDto(
								UPDATED_RANK,
								UPDATED_OFFICE));
	}

	@Test
	@DisplayName("POST /teachers/{id}/update with invalid office -> redirects to same page, flash validation data, service not called")
	void updateTeacher_invalidOffice_redirectsWithCardError()
			throws Exception {

		mockMvc.perform(
				post(
						"/teachers/{id}/update",
						TEACHER_ID)
						.with(user(
								Long.toString(ADMIN_ID))
								.roles("ADMIN"))
						.with(csrf())
						.param(
								"academicRank",
								UPDATED_RANK.name())
						.param(
								"office",
								INVALID_OFFICE)
						.param(
								"page",
								"2"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl(
						"/teachers?page=2"))
				.andExpect(flash().attributeExists(
						"err"))
				.andExpect(flash().attribute(
						"teacherId",
						TEACHER_ID))
				.andExpect(flash().attribute(
						"requestedAcademicRank",
						UPDATED_RANK))
				.andExpect(flash().attribute(
						"requestedOffice",
						INVALID_OFFICE));

		verify(
				teacherService,
				never())
				.update(
						anyLong(),
						any());
	}

	@Test
	@DisplayName("POST /teachers/{id}/update when teacher not found -> redirects, flash err")
	void updateTeacher_notFound_redirectsWithError()
			throws Exception {

		var message =
				"Teacher not found: id="
						+ TEACHER_ID;

		var dto = new TeacherUpdateDto(
				UPDATED_RANK,
				UPDATED_OFFICE);

		when(teacherService.update(
				TEACHER_ID,
				dto))
				.thenThrow(
						new EntityNotFoundException(
								message));

		mockMvc.perform(
				post(
						"/teachers/{id}/update",
						TEACHER_ID)
						.with(user(
								Long.toString(ADMIN_ID))
								.roles("ADMIN"))
						.with(csrf())
						.param(
								"academicRank",
								UPDATED_RANK.name())
						.param(
								"office",
								UPDATED_OFFICE)
						.param(
								"page",
								"0"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl(
						"/teachers"))
				.andExpect(flash().attribute(
						"err",
						message));

		verify(teacherService)
				.update(
						TEACHER_ID,
						dto);
	}

	@Test
	@DisplayName("POST /teachers/{id}/update as teacher -> 403, service not called")
	void updateTeacher_teacher_forbidden()
			throws Exception {

		mockMvc.perform(
				post(
						"/teachers/{id}/update",
						TEACHER_ID)
						.with(user(
								Long.toString(
										TEACHER_USER_ID))
								.roles("TEACHER"))
						.with(csrf())
						.param(
								"academicRank",
								UPDATED_RANK.name())
						.param(
								"office",
								UPDATED_OFFICE))
				.andExpect(status().isForbidden());

		verify(
				teacherService,
				never())
				.update(
						anyLong(),
						any());
	}

	@Test
	@DisplayName("POST /teachers/{id}/update as student -> 403, service not called")
	void updateTeacher_student_forbidden()
			throws Exception {

		mockMvc.perform(
				post(
						"/teachers/{id}/update",
						TEACHER_ID)
						.with(user(
								Long.toString(
										STUDENT_USER_ID))
								.roles("STUDENT"))
						.with(csrf())
						.param(
								"academicRank",
								UPDATED_RANK.name())
						.param(
								"office",
								UPDATED_OFFICE))
				.andExpect(status().isForbidden());

		verify(
				teacherService,
				never())
				.update(
						anyLong(),
						any());
	}

	@Test
	@DisplayName("POST /teachers/{id}/update unauthenticated -> 401, service not called")
	void updateTeacher_unauthenticated_unauthorized()
			throws Exception {

		mockMvc.perform(
				post(
						"/teachers/{id}/update",
						TEACHER_ID)
						.with(csrf())
						.param(
								"academicRank",
								UPDATED_RANK.name())
						.param(
								"office",
								UPDATED_OFFICE))
				.andExpect(status().isUnauthorized());

		verify(
				teacherService,
				never())
				.update(
						anyLong(),
						any());
	}
}