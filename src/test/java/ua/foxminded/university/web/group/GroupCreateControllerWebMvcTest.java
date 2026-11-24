package ua.foxminded.university.web.group;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
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
        MethodSecurityTestConfig.class,
        ExceptionMessageReader.class
})
class GroupCreateControllerWebMvcTest {

    private static final long ADMIN_ID = 41L;
    private static final long TEACHER_ID = 42L;
    private static final long STUDENT_ID = 43L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StudyGroupService groupService;

    @MockitoBean
    private TeacherService teacherService;

	@Test
	@DisplayName("GET /groups/create as admin -> 200, groups/create, model contains form")
	void getCreateGroup_admin_ok()
			throws Exception {

		var result = mockMvc.perform(
				get("/groups/create")
						.with(user(
								Long.toString(ADMIN_ID))
								.roles("ADMIN")))
				.andExpect(status().isOk())
				.andExpect(view().name(
						"groups/create"))
				.andExpect(model().attributeExists(
						"form"))
				.andReturn();

		var form = result.getModelAndView()
				.getModel()
				.get("form");

		assertInstanceOf(
				StudyGroupCreateDto.class,
				form);
	}

	@Test
	@DisplayName("GET /groups/create as teacher -> 403")
	void getCreateGroup_teacher_forbidden()
			throws Exception {

		mockMvc.perform(
				get("/groups/create")
						.with(user(
								Long.toString(TEACHER_ID))
								.roles("TEACHER")))
				.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("GET /groups/create as student -> 403")
	void getCreateGroup_student_forbidden()
			throws Exception {

		mockMvc.perform(
				get("/groups/create")
						.with(user(
								Long.toString(STUDENT_ID))
								.roles("STUDENT")))
				.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("GET /groups/create unauthenticated -> 401")
	void getCreateGroup_unauthenticated_unauthorized()
			throws Exception {

		mockMvc.perform(
				get("/groups/create"))
				.andExpect(status().isUnauthorized());
	}
}