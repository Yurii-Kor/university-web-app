package ua.foxminded.university.service.rolechange;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import jakarta.persistence.EntityNotFoundException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import ua.foxminded.university.TestcontainersConfiguration;
import ua.foxminded.university.model.domain.enums.UserRole;
import ua.foxminded.university.service.rolechange.assessment.RoleChangeAssessmentMode;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
@Sql(
        statements = {
                """
                insert into app_user (id, email, password, role, first_name, last_name, enabled)
                values (1001, 'assessment.student@example.com', 'Abcd1234!', 'STUDENT', 'Role', 'Assessor', true);
                """,
                """
                insert into app_user (id, email, password, role, first_name, last_name, enabled)
                values (1002, 'assessment.teacher@example.com', 'Abcd1234!', 'TEACHER', 'Role', 'Assessor', true);
                """,
                """
                insert into groups (id, name)
                values (2001, 'RPC-101');
                """,
                """
                insert into student (id, group_id, enrollment_year)
                values (1001, 2001, 2024);
                """,
                """
                insert into teacher (id, academic_rank, office)
                values (1002, 'LECTURER', 'A-101');
                """
        },
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
@Sql(
        scripts = "/sql/cleanup-database.sql",
        executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
)
class RoleChangeAssessmentServiceTest {

    private static final long STUDENT_USER_ID = 1001L;
    private static final long TEACHER_USER_ID = 1002L;
    private static final long MISSING_ID = 999_999L;

    @Autowired RoleChangeAssessmentService assessmentService;

    @Test
    @DisplayName("assessRoleChange: student -> teacher without deleted teacher profile -> input required")
    void assessRoleChange_studentToTeacher_noDeletedTeacher_inputRequired() {
        var assessment = assessmentService.assessRoleChange(
                STUDENT_USER_ID,
                UserRole.STUDENT,
                UserRole.TEACHER
        );

        assertEquals(STUDENT_USER_ID, assessment.userId());
        assertEquals(UserRole.STUDENT, assessment.sourceRole());
        assertEquals(UserRole.TEACHER, assessment.targetRole());
        assertEquals(RoleChangeAssessmentMode.INPUT_REQUIRED, assessment.mode());
        assertEquals(List.of("academicRank", "office"), assessment.requiredFields());
    }

    @Test
    @Sql(
            statements = """
                    insert into teacher (id, academic_rank, office, deleted_at)
                    values (1001, 'LECTURER', 'T-restore', now());
                    """,
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
    )
    @DisplayName("assessRoleChange: student -> teacher with deleted teacher profile -> auto restore available")
    void assessRoleChange_studentToTeacher_deletedTeacher_autoRestoreAvailable() {
        var assessment = assessmentService.assessRoleChange(
                STUDENT_USER_ID,
                UserRole.STUDENT,
                UserRole.TEACHER
        );

        assertEquals(STUDENT_USER_ID, assessment.userId());
        assertEquals(UserRole.STUDENT, assessment.sourceRole());
        assertEquals(UserRole.TEACHER, assessment.targetRole());
        assertEquals(RoleChangeAssessmentMode.AUTO_RESTORE_AVAILABLE, assessment.mode());
        assertTrue(assessment.requiredFields().isEmpty());
    }

    @Test
    @DisplayName("assessRoleChange: teacher -> student without deleted student profile -> input required")
    void assessRoleChange_teacherToStudent_noDeletedStudent_inputRequired() {
        var assessment = assessmentService.assessRoleChange(
                TEACHER_USER_ID,
                UserRole.TEACHER,
                UserRole.STUDENT
        );

        assertEquals(TEACHER_USER_ID, assessment.userId());
        assertEquals(UserRole.TEACHER, assessment.sourceRole());
        assertEquals(UserRole.STUDENT, assessment.targetRole());
        assertEquals(RoleChangeAssessmentMode.INPUT_REQUIRED, assessment.mode());
        assertEquals(List.of("groupId", "enrollmentYear"), assessment.requiredFields());
    }

    @Test
    @Sql(
            statements = """
                    insert into student (id, group_id, enrollment_year, deleted_at)
                    values (1002, 2001, 2024, now());
                    """,
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
    )
    @DisplayName("assessRoleChange: teacher -> student with deleted student profile and active group -> auto restore available")
    void assessRoleChange_teacherToStudent_deletedStudentWithActiveGroup_autoRestoreAvailable() {
        var assessment = assessmentService.assessRoleChange(
                TEACHER_USER_ID,
                UserRole.TEACHER,
                UserRole.STUDENT
        );

        assertEquals(TEACHER_USER_ID, assessment.userId());
        assertEquals(UserRole.TEACHER, assessment.sourceRole());
        assertEquals(UserRole.STUDENT, assessment.targetRole());
        assertEquals(RoleChangeAssessmentMode.AUTO_RESTORE_AVAILABLE, assessment.mode());
        assertTrue(assessment.requiredFields().isEmpty());
    }

    @Test
    @Sql(
            statements = {
                    """
                    insert into groups (id, name, deleted_at)
                    values (2002, 'RPC-ARCHIVED', now());
                    """,
                    """
                    insert into student (id, group_id, enrollment_year, deleted_at)
                    values (1002, 2002, 2024, now());
                    """
            },
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
    )
    @DisplayName("assessRoleChange: teacher -> student with deleted student profile and archived group -> input required")
    void assessRoleChange_teacherToStudent_deletedStudentWithArchivedGroup_inputRequired() {
        var assessment = assessmentService.assessRoleChange(
                TEACHER_USER_ID,
                UserRole.TEACHER,
                UserRole.STUDENT
        );

        assertEquals(TEACHER_USER_ID, assessment.userId());
        assertEquals(UserRole.TEACHER, assessment.sourceRole());
        assertEquals(UserRole.STUDENT, assessment.targetRole());
        assertEquals(RoleChangeAssessmentMode.INPUT_REQUIRED, assessment.mode());
        assertEquals(List.of("groupId", "enrollmentYear"), assessment.requiredFields());
    }

    @Test
    @DisplayName("assessRoleChange: target role ADMIN -> blocked")
    void assessRoleChange_targetAdmin_blocked() {
        var assessment = assessmentService.assessRoleChange(
                STUDENT_USER_ID,
                UserRole.STUDENT,
                UserRole.ADMIN
        );

        assertEquals(STUDENT_USER_ID, assessment.userId());
        assertEquals(UserRole.STUDENT, assessment.sourceRole());
        assertEquals(UserRole.ADMIN, assessment.targetRole());
        assertEquals(RoleChangeAssessmentMode.BLOCKED, assessment.mode());
        assertTrue(assessment.requiredFields().isEmpty());
    }

    @Test
    @DisplayName("assessRoleChange: missing account -> EntityNotFoundException")
    void assessRoleChange_missingAccount_fails() {
        assertThrows(EntityNotFoundException.class,
                () -> assessmentService.assessRoleChange(MISSING_ID, UserRole.STUDENT, UserRole.TEACHER));
    }

    @Test
    @DisplayName("assessRoleChange: expected source role does not match actual role -> IllegalStateException")
    void assessRoleChange_sourceRoleMismatch_fails() {
        assertThrows(IllegalStateException.class,
                () -> assessmentService.assessRoleChange(STUDENT_USER_ID, UserRole.TEACHER, UserRole.STUDENT));
    }

    @Test
    @DisplayName("assessRoleChange: target role equals current role -> IllegalArgumentException")
    void assessRoleChange_sameTargetRole_fails() {
        assertThrows(IllegalArgumentException.class,
                () -> assessmentService.assessRoleChange(STUDENT_USER_ID, UserRole.STUDENT, UserRole.STUDENT));
    }
}