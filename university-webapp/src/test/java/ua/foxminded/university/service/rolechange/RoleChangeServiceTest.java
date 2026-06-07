package ua.foxminded.university.service.rolechange;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import ua.foxminded.university.TestcontainersConfiguration;
import ua.foxminded.university.model.domain.enums.AcademicRank;
import ua.foxminded.university.model.domain.enums.UserRole;
import ua.foxminded.university.model.repository.AppUserRepository;
import ua.foxminded.university.model.repository.StudentRepository;
import ua.foxminded.university.model.repository.TeacherRepository;
import ua.foxminded.university.service.dto.request.rolechange.ToStudentRoleChangeDto;
import ua.foxminded.university.service.dto.request.rolechange.ToTeacherRoleChangeDto;
import ua.foxminded.university.service.rolechange.exception.RoleChangeException;
import ua.foxminded.university.service.rolechange.target.strategy.StudentTargetRoleProfileHandler;
import ua.foxminded.university.service.rolechange.target.strategy.TeacherTargetRoleProfileHandler;


@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
@Sql(
        statements = {
                """
                insert into app_user (id, email, password, role, first_name, last_name, enabled)
                values (1001, 'role.student@example.com', 'Abcd1234!', 'STUDENT', 'Role', 'Changer', true);
                """,
                """
                insert into app_user (id, email, password, role, first_name, last_name, enabled)
                values (1002, 'role.teacher@example.com', 'Abcd1234!', 'TEACHER', 'Role', 'Changer', true);
                """,
                """
                insert into groups (id, name)
                values (2001, 'RC-101');
                """,
                """
                insert into student (id, group_id, enrollment_year)
                values (1001, 2001, 2024);
                """,
                """
                insert into teacher (id, academic_rank, office)
                values (1002, 'LECTURER', 'A-101');
                """,
                """
                insert into app_user (id, email, password, role, first_name, last_name, enabled)
                values (1003, 'role.admin@example.com', 'Abcd1234!', 'ADMIN', 'Role', 'Admin', true);
                """
        },
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
@Sql(
        scripts = "/sql/cleanup-database.sql",
        executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
)
class RoleChangeServiceTest {
    
    static final long STUDENT_USER_ID = 1001L;
    static final long TEACHER_USER_ID = 1002L;
    static final long ADMIN_USER_ID   = 1003L;
    static final long ACTIVE_GROUP_ID = 2001L;

    static final String OFFICE_A = "A-101";
    static final String OFFICE_B = "B-202";

    static final Integer ENROLLMENT_YEAR = 2024;
    
    @Autowired RoleChangeService roleChangeService;

    @Autowired AppUserRepository userRepository;
    @Autowired StudentRepository studentRepository;
    @Autowired TeacherRepository teacherRepository;

    @Autowired StudentTargetRoleProfileHandler studentTargetHandler;
    @Autowired TeacherTargetRoleProfileHandler teacherTargetHandler;

    @Test
    @DisplayName("performRoleChange: student -> teacher creates teacher profile, soft-deletes student profile, updates user role")
    void performRoleChange_studentToTeacher_createsTeacher_softDeletesStudent_updatesRole() {
        var form = toTeacherForm(AcademicRank.LECTURER, OFFICE_A);

        roleChangeService.performRoleChange(
                STUDENT_USER_ID,
                UserRole.STUDENT,
                teacherTargetHandler,
                form
        );

        assertUserRole(STUDENT_USER_ID, UserRole.TEACHER);

        assertTrue(studentRepository.findById(STUDENT_USER_ID).isEmpty());
        assertTrue(studentRepository.findRestorableDeletedById(STUDENT_USER_ID).isPresent());
        assertTrue(teacherRepository.findById(STUDENT_USER_ID).isPresent());

        var teacher = teacherRepository.findById(STUDENT_USER_ID).orElseThrow();
        assertEquals(AcademicRank.LECTURER, teacher.getAcademicRank());
        assertEquals(OFFICE_A, teacher.getOffice());
    }

    @Test
    @DisplayName("performRoleChange: teacher -> student creates student profile, soft-deletes teacher profile, updates user role")
    void performRoleChange_teacherToStudent_createsStudent_softDeletesTeacher_updatesRole() {
        var form = toStudentForm(ACTIVE_GROUP_ID, ENROLLMENT_YEAR);

        roleChangeService.performRoleChange(
                TEACHER_USER_ID,
                UserRole.TEACHER,
                studentTargetHandler,
                form
        );

        assertUserRole(TEACHER_USER_ID, UserRole.STUDENT);

        assertTrue(teacherRepository.findById(TEACHER_USER_ID).isEmpty());
        assertTrue(teacherRepository.findDeletedById(TEACHER_USER_ID).isPresent());
        assertTrue(studentRepository.findById(TEACHER_USER_ID).isPresent());

        var student = studentRepository.findById(TEACHER_USER_ID).orElseThrow();
        assertEquals(ACTIVE_GROUP_ID, student.getGroup().getId());
        assertEquals(ENROLLMENT_YEAR, student.getEnrollmentYear());
    }

    @Test
    @Sql(
            statements = """
                    insert into teacher (id, academic_rank, office, deleted_at)
                    values (1001, 'PROFESSOR', 'B-202', now());
                    """,
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
    )
    @DisplayName("performRoleChange: student -> teacher restores deleted teacher profile, soft-deletes student profile, updates user role")
    void performRoleChange_studentToTeacher_restoresDeletedTeacher_softDeletesStudent_updatesRole() {
        roleChangeService.performRoleChange(
                STUDENT_USER_ID,
                UserRole.STUDENT,
                teacherTargetHandler,
                null
        );

        assertUserRole(STUDENT_USER_ID, UserRole.TEACHER);

        assertTrue(studentRepository.findById(STUDENT_USER_ID).isEmpty());
        assertTrue(studentRepository.findRestorableDeletedById(STUDENT_USER_ID).isPresent());

        assertTrue(teacherRepository.findDeletedById(STUDENT_USER_ID).isEmpty());

        var teacher = teacherRepository.findById(STUDENT_USER_ID).orElseThrow();
        assertEquals(AcademicRank.PROFESSOR, teacher.getAcademicRank());
        assertEquals(OFFICE_B, teacher.getOffice());
    }

    @Test
    @Sql(
            statements = """
                    insert into student (id, group_id, enrollment_year, deleted_at)
                    values (1002, 2001, 2024, now());
                    """,
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
    )
    @DisplayName("performRoleChange: teacher -> student restores deleted student profile when previous group is active")
    void performRoleChange_teacherToStudent_restoresDeletedStudentWithActiveGroup_updatesRole() {
        roleChangeService.performRoleChange(
                TEACHER_USER_ID,
                UserRole.TEACHER,
                studentTargetHandler,
                null
        );

        assertUserRole(TEACHER_USER_ID, UserRole.STUDENT);

        assertTrue(teacherRepository.findById(TEACHER_USER_ID).isEmpty());
        assertTrue(teacherRepository.findDeletedById(TEACHER_USER_ID).isPresent());

        assertTrue(studentRepository.findRestorableDeletedById(TEACHER_USER_ID).isEmpty());

        var student = studentRepository.findById(TEACHER_USER_ID).orElseThrow();
        assertEquals(ACTIVE_GROUP_ID, student.getGroup().getId());
        assertEquals(ENROLLMENT_YEAR, student.getEnrollmentYear());
    }

    @Test
    @Sql(
            statements = {
                    """
                    insert into groups (id, name, deleted_at)
                    values (2002, 'RC-ARCHIVED', now());
                    """,
                    """
                    insert into student (id, group_id, enrollment_year, deleted_at)
                    values (1002, 2002, 2024, now());
                    """
            },
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
    )
    @DisplayName("performRoleChange: teacher -> student with archived previous group fails and leaves DB unchanged")
    void performRoleChange_teacherToStudent_archivedPreviousGroup_failsAndLeavesDbUnchanged() {
        assertThrows(RoleChangeException.class,
                () -> roleChangeService.performRoleChange(
                        TEACHER_USER_ID,
                        UserRole.TEACHER,
                        studentTargetHandler,
                        null
                ));

        assertUserRole(TEACHER_USER_ID, UserRole.TEACHER);

        assertTrue(teacherRepository.findById(TEACHER_USER_ID).isPresent());
        assertTrue(teacherRepository.findDeletedById(TEACHER_USER_ID).isEmpty());

        assertTrue(studentRepository.findById(TEACHER_USER_ID).isEmpty());
        assertTrue(studentRepository.findRestorableDeletedById(TEACHER_USER_ID).isEmpty());
    }

    @Test
    @Sql(
            statements = """
                    insert into courses (id, code, name, description, teacher_id)
                    values (3001, 'RC-COURSE-204', 'Role Change Locked Course', null, 1002);
                    """,
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
    )
    @DisplayName("performRoleChange: teacher -> student with assigned courses fails and rolls back")
    void performRoleChange_teacherToStudent_teacherWithCourses_failsAndRollsBack() {
        var form = toStudentForm(ACTIVE_GROUP_ID, ENROLLMENT_YEAR);

        assertThrows(RoleChangeException.class,
                () -> roleChangeService.performRoleChange(
                        TEACHER_USER_ID,
                        UserRole.TEACHER,
                        studentTargetHandler,
                        form
                ));

        assertUserRole(TEACHER_USER_ID, UserRole.TEACHER);

        assertTrue(teacherRepository.findById(TEACHER_USER_ID).isPresent());
        assertTrue(teacherRepository.findDeletedById(TEACHER_USER_ID).isEmpty());

        assertTrue(studentRepository.findById(TEACHER_USER_ID).isEmpty());
        assertTrue(studentRepository.findRestorableDeletedById(TEACHER_USER_ID).isEmpty());
    }

    @Test
    @DisplayName("performRoleChange: source role mismatch fails and leaves DB unchanged")
    void performRoleChange_sourceRoleMismatch_failsAndLeavesDbUnchanged() {
        var form = toTeacherForm(AcademicRank.LECTURER, OFFICE_A);

        assertThrows(RoleChangeException.class,
                () -> roleChangeService.performRoleChange(
                        STUDENT_USER_ID,
                        UserRole.TEACHER,
                        teacherTargetHandler,
                        form
                ));

        assertUserRole(STUDENT_USER_ID, UserRole.STUDENT);

        assertTrue(studentRepository.findById(STUDENT_USER_ID).isPresent());
        assertTrue(studentRepository.findRestorableDeletedById(STUDENT_USER_ID).isEmpty());

        assertTrue(teacherRepository.findById(STUDENT_USER_ID).isEmpty());
        assertTrue(teacherRepository.findDeletedById(STUDENT_USER_ID).isEmpty());
    }

    @Test
    @DisplayName("performRoleChange: target role same as current role fails and leaves DB unchanged")
    void performRoleChange_sameTargetRole_failsAndLeavesDbUnchanged() {
        var form = toStudentForm(ACTIVE_GROUP_ID, ENROLLMENT_YEAR);

        assertThrows(RoleChangeException.class,
                () -> roleChangeService.performRoleChange(
                        STUDENT_USER_ID,
                        UserRole.STUDENT,
                        studentTargetHandler,
                        form
                ));

        assertUserRole(STUDENT_USER_ID, UserRole.STUDENT);

        assertTrue(studentRepository.findById(STUDENT_USER_ID).isPresent());
        assertTrue(studentRepository.findRestorableDeletedById(STUDENT_USER_ID).isEmpty());

        assertTrue(teacherRepository.findById(STUDENT_USER_ID).isEmpty());
        assertTrue(teacherRepository.findDeletedById(STUDENT_USER_ID).isEmpty());
    }

    @Test
    @DisplayName("performRoleChange: admin -> student fails and leaves admin role unchanged")
    void performRoleChange_adminToStudent_failsAndLeavesAdminRoleUnchanged() {
        var form = toStudentForm(ACTIVE_GROUP_ID, ENROLLMENT_YEAR);

        assertThrows(IllegalStateException.class,
                () -> roleChangeService.performRoleChange(
                        ADMIN_USER_ID,
                        UserRole.ADMIN,
                        studentTargetHandler,
                        form
                ));

        assertUserRole(ADMIN_USER_ID, UserRole.ADMIN);

        assertTrue(studentRepository.findById(ADMIN_USER_ID).isEmpty());
        assertTrue(studentRepository.findRestorableDeletedById(ADMIN_USER_ID).isEmpty());

        assertTrue(teacherRepository.findById(ADMIN_USER_ID).isEmpty());
        assertTrue(teacherRepository.findDeletedById(ADMIN_USER_ID).isEmpty());
    }

    @Test
    @DisplayName("performRoleChange: student -> teacher without form and deleted teacher profile fails and leaves DB unchanged")
    void performRoleChange_studentToTeacher_noFormAndNoDeletedTeacher_failsAndLeavesDbUnchanged() {
        assertThrows(RoleChangeException.class,
                () -> roleChangeService.performRoleChange(
                        STUDENT_USER_ID,
                        UserRole.STUDENT,
                        teacherTargetHandler,
                        null
                ));

        assertUserRole(STUDENT_USER_ID, UserRole.STUDENT);

        assertTrue(studentRepository.findById(STUDENT_USER_ID).isPresent());
        assertTrue(studentRepository.findRestorableDeletedById(STUDENT_USER_ID).isEmpty());

        assertTrue(teacherRepository.findById(STUDENT_USER_ID).isEmpty());
        assertTrue(teacherRepository.findDeletedById(STUDENT_USER_ID).isEmpty());
    }

    @Test
    @DisplayName("performRoleChange: teacher -> student without form and deleted student profile fails and leaves DB unchanged")
    void performRoleChange_teacherToStudent_noFormAndNoDeletedStudent_failsAndLeavesDbUnchanged() {
        assertThrows(RoleChangeException.class,
                () -> roleChangeService.performRoleChange(
                        TEACHER_USER_ID,
                        UserRole.TEACHER,
                        studentTargetHandler,
                        null
                ));

        assertUserRole(TEACHER_USER_ID, UserRole.TEACHER);

        assertTrue(teacherRepository.findById(TEACHER_USER_ID).isPresent());
        assertTrue(teacherRepository.findDeletedById(TEACHER_USER_ID).isEmpty());

        assertTrue(studentRepository.findById(TEACHER_USER_ID).isEmpty());
        assertTrue(studentRepository.findRestorableDeletedById(TEACHER_USER_ID).isEmpty());
    }

    private ToTeacherRoleChangeDto toTeacherForm(AcademicRank rank, String office) {
        return new ToTeacherRoleChangeDto(rank, office);
    }

    private ToStudentRoleChangeDto toStudentForm(Long groupId, Integer enrollmentYear) {
        return new ToStudentRoleChangeDto(groupId, enrollmentYear);
    }

    private void assertUserRole(long userId, UserRole expectedRole) {
        var user = userRepository.findById(userId).orElseThrow();

        assertEquals(expectedRole, user.getRole());
    }
}
