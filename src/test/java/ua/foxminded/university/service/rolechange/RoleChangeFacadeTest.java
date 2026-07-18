package ua.foxminded.university.service.rolechange;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ua.foxminded.university.model.domain.enums.UserRole;
import ua.foxminded.university.service.rolechange.assessment.RoleChangeAssessment;
import ua.foxminded.university.service.rolechange.assessment.RoleChangeAssessmentMode;
import ua.foxminded.university.service.rolechange.target.TargetRoleProfileHandlerRegistry;
import ua.foxminded.university.service.rolechange.target.strategy.StudentTargetRoleProfileHandler;
import ua.foxminded.university.service.rolechange.target.strategy.TeacherTargetRoleProfileHandler;
import ua.foxminded.university.service.rolechange.target.strategy.data.ToStudentRoleProfileData;
import ua.foxminded.university.service.rolechange.target.strategy.data.ToTeacherRoleProfileData;

@ExtendWith(MockitoExtension.class)
class RoleChangeFacadeTest {

    private static final long USER_ID = 101L;

    @Mock RoleChangeAssessmentService assessmentService;
    @Mock RoleChangeService roleChangeService;

    @Mock StudentTargetRoleProfileHandler studentTargetHandler;
    @Mock TeacherTargetRoleProfileHandler teacherTargetHandler;
    @Mock TargetRoleProfileHandlerRegistry targetHandlerRegistry;

    @Mock ToStudentRoleProfileData studentData;
    @Mock ToTeacherRoleProfileData teacherData;

    @InjectMocks RoleChangeFacade facade;

    @Test
    @DisplayName("assessRoleChange: delegates to assessment service and returns result")
    void assessRoleChange_delegatesToAssessmentServiceAndReturnsResult() {
        var expected = new RoleChangeAssessment(
                USER_ID,
                UserRole.STUDENT,
                UserRole.TEACHER,
                RoleChangeAssessmentMode.INPUT_REQUIRED,
                "Teacher profile data is required.",
                List.of("academicRank", "office")
        );

        when(assessmentService.assessRoleChange(USER_ID, UserRole.STUDENT, UserRole.TEACHER))
                .thenReturn(expected);

        var actual = facade.assessRoleChange(USER_ID, UserRole.STUDENT, UserRole.TEACHER);

        assertSame(expected, actual);
        verify(assessmentService).assessRoleChange(USER_ID, UserRole.STUDENT, UserRole.TEACHER);
        verifyNoInteractions(roleChangeService);
    }

    @Test
    @DisplayName("changeRoleToStudent: delegates to role change service with student target handler")
    void changeRoleToStudent_delegatesWithStudentTargetHandler() {
        facade.changeRoleToStudent(USER_ID, UserRole.TEACHER, studentData);

        verify(roleChangeService).performRoleChange(
                USER_ID,
                UserRole.TEACHER,
                studentTargetHandler,
                studentData
        );
        verifyNoInteractions(assessmentService);
    }

    @Test
    @DisplayName("changeRoleToTeacher: delegates to role change service with teacher target handler")
    void changeRoleToTeacher_delegatesWithTeacherTargetHandler() {
        facade.changeRoleToTeacher(USER_ID, UserRole.STUDENT, teacherData);

        verify(roleChangeService).performRoleChange(
                USER_ID,
                UserRole.STUDENT,
                teacherTargetHandler,
                teacherData
        );
        verifyNoInteractions(assessmentService);
    }

    @Test
    @DisplayName("restoreRole: resolves target handler and delegates with null target data")
    void restoreRole_resolvesTargetHandlerAndDelegatesWithNullTargetData() {
        doReturn(teacherTargetHandler)
                .when(targetHandlerRegistry)
                .getRequired(UserRole.TEACHER);

        facade.restoreRole(USER_ID, UserRole.STUDENT, UserRole.TEACHER);

        verify(targetHandlerRegistry).getRequired(UserRole.TEACHER);
        verify(roleChangeService).performRoleChange(
                USER_ID,
                UserRole.STUDENT,
                teacherTargetHandler,
                null
        );
        verifyNoInteractions(assessmentService);
    }
}