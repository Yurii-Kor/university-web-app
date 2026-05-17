package ua.foxminded.university.service.rolechange;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import ua.foxminded.university.TestcontainersConfiguration;
import ua.foxminded.university.model.domain.AppUser;
import ua.foxminded.university.model.domain.Student;
import ua.foxminded.university.model.domain.StudyGroup;
import ua.foxminded.university.model.domain.Teacher;
import ua.foxminded.university.model.domain.enums.AcademicRank;
import ua.foxminded.university.model.domain.enums.UserRole;
import ua.foxminded.university.service.rolechange.assessment.RoleChangeAssessmentMode;
import ua.foxminded.university.service.rolechange.assessment.TargetRoleChangeAssessorRegistry;
import ua.foxminded.university.service.rolechange.assessment.strategy.AdminTargetRoleChangeAssessor;
import ua.foxminded.university.service.rolechange.assessment.strategy.StudentTargetRoleChangeAssessor;
import ua.foxminded.university.service.rolechange.assessment.strategy.TeacherTargetRoleChangeAssessor;
import ua.foxminded.university.testutil.TestDataInitializer;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
        TestcontainersConfiguration.class,
        TestDataInitializer.class,
        RoleChangeAssessmentService.class,
        TargetRoleChangeAssessorRegistry.class,
        AdminTargetRoleChangeAssessor.class,
        StudentTargetRoleChangeAssessor.class,
        TeacherTargetRoleChangeAssessor.class
})
class RoleChangeAssessmentServiceTest {

    private static final String PASSWORD = "Abcd1234!";
    private static final String FIRST_NAME = "Role";
    private static final String LAST_NAME = "Assessor";

    private static final Integer ENROLLMENT_YEAR = 2024;
    private static final Long MISSING_ID = 999_999L;
    
    @PersistenceContext EntityManager em;

    @Autowired TestDataInitializer initializer;
    @Autowired RoleChangeAssessmentService assessmentService;

    @Test
    @DisplayName("assessRoleChange: student -> teacher without deleted teacher profile -> input required")
    void assessRoleChange_studentToTeacher_noDeletedTeacher_inputRequired() {
        var student = activeStudent("assessment.student.to.teacher.input@example.com", "RPC-101");

        var assessment = assessmentService.assessRoleChange(
                student.getId(),
                UserRole.STUDENT,
                UserRole.TEACHER
        );

        assertEquals(student.getId(), assessment.userId());
        assertEquals(UserRole.STUDENT, assessment.sourceRole());
        assertEquals(UserRole.TEACHER, assessment.targetRole());
        assertEquals(RoleChangeAssessmentMode.INPUT_REQUIRED, assessment.mode());
        assertEquals(List.of("academicRank", "office"), assessment.requiredFields());
    }

    @Test
    @DisplayName("assessRoleChange: student -> teacher with deleted teacher profile -> auto restore available")
    void assessRoleChange_studentToTeacher_deletedTeacher_autoRestoreAvailable() {
        var student = activeStudent("assessment.student.to.teacher.restore@example.com", "RPC-102");

        insertDeletedTeacherProfile(student.getId());

        var assessment = assessmentService.assessRoleChange(
                student.getId(),
                UserRole.STUDENT,
                UserRole.TEACHER
        );

        assertEquals(student.getId(), assessment.userId());
        assertEquals(UserRole.STUDENT, assessment.sourceRole());
        assertEquals(UserRole.TEACHER, assessment.targetRole());
        assertEquals(RoleChangeAssessmentMode.AUTO_RESTORE_AVAILABLE, assessment.mode());
        assertTrue(assessment.requiredFields().isEmpty());
    }

    @Test
    @DisplayName("assessRoleChange: teacher -> student without deleted student profile -> input required")
    void assessRoleChange_teacherToStudent_noDeletedStudent_inputRequired() {
        var teacher = activeTeacher("assessment.teacher.to.student.input@example.com");

        var assessment = assessmentService.assessRoleChange(
                teacher.getId(),
                UserRole.TEACHER,
                UserRole.STUDENT
        );

        assertEquals(teacher.getId(), assessment.userId());
        assertEquals(UserRole.TEACHER, assessment.sourceRole());
        assertEquals(UserRole.STUDENT, assessment.targetRole());
        assertEquals(RoleChangeAssessmentMode.INPUT_REQUIRED, assessment.mode());
        assertEquals(List.of("groupId", "enrollmentYear"), assessment.requiredFields());
    }

    @Test
    @DisplayName("assessRoleChange: teacher -> student with deleted student profile and active group -> auto restore available")
    void assessRoleChange_teacherToStudent_deletedStudentWithActiveGroup_autoRestoreAvailable() {
        var teacher = activeTeacher("assessment.teacher.to.student.restore@example.com");
        var group = activeGroup("RPC-201");

        insertDeletedStudentProfile(teacher.getId(), group.getId());

        var assessment = assessmentService.assessRoleChange(
                teacher.getId(),
                UserRole.TEACHER,
                UserRole.STUDENT
        );

        assertEquals(teacher.getId(), assessment.userId());
        assertEquals(UserRole.TEACHER, assessment.sourceRole());
        assertEquals(UserRole.STUDENT, assessment.targetRole());
        assertEquals(RoleChangeAssessmentMode.AUTO_RESTORE_AVAILABLE, assessment.mode());
        assertTrue(assessment.requiredFields().isEmpty());
    }

    @Test
    @DisplayName("assessRoleChange: teacher -> student with deleted student profile and archived group -> input required")
    void assessRoleChange_teacherToStudent_deletedStudentWithArchivedGroup_inputRequired() {
        var teacher = activeTeacher("assessment.teacher.to.student.archived.group@example.com");
        var group = activeGroup("RPC-202");

        softDeleteGroup(group.getId());
        insertDeletedStudentProfile(teacher.getId(), group.getId());

        var assessment = assessmentService.assessRoleChange(
                teacher.getId(),
                UserRole.TEACHER,
                UserRole.STUDENT
        );

        assertEquals(teacher.getId(), assessment.userId());
        assertEquals(UserRole.TEACHER, assessment.sourceRole());
        assertEquals(UserRole.STUDENT, assessment.targetRole());
        assertEquals(RoleChangeAssessmentMode.INPUT_REQUIRED, assessment.mode());
        assertEquals(List.of("groupId", "enrollmentYear"), assessment.requiredFields());
    }

    @Test
    @DisplayName("assessRoleChange: target role ADMIN -> blocked")
    void assessRoleChange_targetAdmin_blocked() {
        var student = activeStudent("assessment.student.to.admin.blocked@example.com", "RPC-301");

        var assessment = assessmentService.assessRoleChange(
                student.getId(),
                UserRole.STUDENT,
                UserRole.ADMIN
        );

        assertEquals(student.getId(), assessment.userId());
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
        var student = activeStudent("assessment.source.mismatch@example.com", "RPC-401");

        assertThrows(IllegalStateException.class,
                () -> assessmentService.assessRoleChange(student.getId(), UserRole.TEACHER, UserRole.STUDENT));
    }

    @Test
    @DisplayName("assessRoleChange: target role equals current role -> IllegalArgumentException")
    void assessRoleChange_sameTargetRole_fails() {
        var student = activeStudent("assessment.same.role@example.com", "RPC-402");

        assertThrows(IllegalArgumentException.class,
                () -> assessmentService.assessRoleChange(student.getId(), UserRole.STUDENT, UserRole.STUDENT));
    }
    
    private Student activeStudent(String email, String groupName) {
        var group = activeGroup(groupName);
        var user = activeUser(email, UserRole.STUDENT);

        return initializer.persistAll(Student.builder()
                .user(user)
                .group(group)
                .enrollmentYear(ENROLLMENT_YEAR)
                .build()).getFirst();
    }

    private Teacher activeTeacher(String email) {
        var user = activeUser(email, UserRole.TEACHER);

        return initializer.persistAll(Teacher.builder()
                .user(user)
                .academicRank(AcademicRank.LECTURER)
                .office("T-101")
                .build()).getFirst();
    }

    private AppUser activeUser(String email, UserRole role) {
        return initializer.persistAll(AppUser.builder()
                .email(email)
                .password(PASSWORD)
                .role(role)
                .firstName(FIRST_NAME)
                .lastName(LAST_NAME)
                .enabled(true)
                .build()).getFirst();
    }

    private StudyGroup activeGroup(String name) {
        return initializer.persistAll(StudyGroup.builder()
                .name(name)
                .build()).getFirst();
    }

    private void insertDeletedTeacherProfile(long userId) {
        em.createNativeQuery("""
                insert into teacher (id, academic_rank, office, deleted_at)
                values (:id, :rank, :office, now())
                """)
                .setParameter("id", userId)
                .setParameter("rank", AcademicRank.LECTURER.name())
                .setParameter("office", "T-restore")
                .executeUpdate();

        em.flush();
        em.clear();
    }

    private void insertDeletedStudentProfile(long userId, long groupId) {
        em.createNativeQuery("""
                insert into student (id, group_id, enrollment_year, deleted_at)
                values (:id, :groupId, :enrollmentYear, now())
                """)
                .setParameter("id", userId)
                .setParameter("groupId", groupId)
                .setParameter("enrollmentYear", ENROLLMENT_YEAR)
                .executeUpdate();

        em.flush();
        em.clear();
    }

    private void softDeleteGroup(long groupId) {
        em.createNativeQuery("""
                update groups
                set deleted_at = now()
                where id = :id
                """)
                .setParameter("id", groupId)
                .executeUpdate();

        em.flush();
        em.clear();
    }
}