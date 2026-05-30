package ua.foxminded.university.service.rolechange;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.transaction.TestTransaction;

import ua.foxminded.university.TestcontainersConfiguration;
import ua.foxminded.university.model.domain.AppUser;
import ua.foxminded.university.model.domain.Course;
import ua.foxminded.university.model.domain.Student;
import ua.foxminded.university.model.domain.StudyGroup;
import ua.foxminded.university.model.domain.Teacher;
import ua.foxminded.university.model.domain.enums.AcademicRank;
import ua.foxminded.university.model.domain.enums.UserRole;
import ua.foxminded.university.model.repository.AppUserRepository;
import ua.foxminded.university.model.repository.StudentRepository;
import ua.foxminded.university.model.repository.TeacherRepository;
import ua.foxminded.university.service.dto.request.rolechange.ToStudentRoleChangeDto;
import ua.foxminded.university.service.dto.request.rolechange.ToTeacherRoleChangeDto;
import ua.foxminded.university.service.rolechange.current.CurrentRoleProfileHandlerRegistry;
import ua.foxminded.university.service.rolechange.current.strategy.AdminCurrentRoleProfileHandler;
import ua.foxminded.university.service.rolechange.current.strategy.StudentCurrentRoleProfileHandler;
import ua.foxminded.university.service.rolechange.current.strategy.TeacherCurrentRoleProfileHandler;
import ua.foxminded.university.service.rolechange.exception.RoleChangeException;
import ua.foxminded.university.service.rolechange.target.strategy.StudentTargetRoleProfileHandler;
import ua.foxminded.university.service.rolechange.target.strategy.TeacherTargetRoleProfileHandler;
import ua.foxminded.university.service.util.validation.EntityValidatior;
import ua.foxminded.university.service.util.validation.config.ValidatorConfig;
import ua.foxminded.university.testutil.TestDataInitializer;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
        TestcontainersConfiguration.class,
        TestDataInitializer.class,

        RoleChangeService.class,

        CurrentRoleProfileHandlerRegistry.class,
        AdminCurrentRoleProfileHandler.class,
        StudentCurrentRoleProfileHandler.class,
        TeacherCurrentRoleProfileHandler.class,

        StudentTargetRoleProfileHandler.class,
        TeacherTargetRoleProfileHandler.class,

        ValidatorConfig.class,
        EntityValidatior.class
})
class RoleChangeServiceTest {

    private static final String PASSWORD = "Abcd1234!";
    private static final String FIRST_NAME = "Role";
    private static final String LAST_NAME = "Changer";

    private static final String OFFICE_A = "T-101";
    private static final String OFFICE_B = "T-202";

    private static final Integer ENROLLMENT_YEAR = 2024;

    @PersistenceContext EntityManager em;

    @Autowired AppUserRepository userRepository;
    @Autowired StudentRepository studentRepository;
    @Autowired TeacherRepository teacherRepository;
    @Autowired TestDataInitializer initializer;

    @Autowired RoleChangeService roleChangeService;
    @Autowired StudentTargetRoleProfileHandler studentTargetHandler;
    @Autowired TeacherTargetRoleProfileHandler teacherTargetHandler;

    @Test
    @DisplayName("performRoleChange: student -> teacher creates teacher profile, soft-deletes student profile, updates user role")
    void performRoleChange_studentToTeacher_createsTeacher_softDeletesStudent_updatesRole() {
        var student = activeStudent("role.student.to.teacher.create@example.com", "RC-101");
        var form = toTeacherForm(AcademicRank.LECTURER, OFFICE_A);

        commitSetupBeforeRequiresNewServiceCall();

        roleChangeService.performRoleChange(
                student.getId(),
                UserRole.STUDENT,
                teacherTargetHandler,
                form
        );

        assertUserRole(student.getId(), UserRole.TEACHER);

        assertTrue(studentRepository.findById(student.getId()).isEmpty());
        assertTrue(teacherRepository.findById(student.getId()).isPresent());

        assertProfileDeleted("student", student.getId());
        assertProfileActive("teacher", student.getId());

        var teacher = teacherRepository.findById(student.getId()).orElseThrow();
        assertEquals(AcademicRank.LECTURER, teacher.getAcademicRank());
        assertEquals(OFFICE_A, teacher.getOffice());
    }

    @Test
    @DisplayName("performRoleChange: student -> teacher restores deleted teacher profile without submitted data")
    void performRoleChange_studentToTeacher_restoresDeletedTeacher_softDeletesStudent_updatesRole() {
        var student = activeStudent("role.student.to.teacher.restore@example.com", "RC-102");

        insertDeletedTeacherProfile(student.getId(), AcademicRank.PROFESSOR, OFFICE_B);

        commitSetupBeforeRequiresNewServiceCall();

        roleChangeService.performRoleChange(
                student.getId(),
                UserRole.STUDENT,
                teacherTargetHandler,
                null
        );

        assertUserRole(student.getId(), UserRole.TEACHER);

        assertTrue(studentRepository.findById(student.getId()).isEmpty());
        assertTrue(teacherRepository.findById(student.getId()).isPresent());

        assertProfileDeleted("student", student.getId());
        assertProfileActive("teacher", student.getId());

        var teacher = teacherRepository.findById(student.getId()).orElseThrow();
        assertEquals(AcademicRank.PROFESSOR, teacher.getAcademicRank());
        assertEquals(OFFICE_B, teacher.getOffice());
    }

    @Test
    @DisplayName("performRoleChange: teacher -> student creates student profile, soft-deletes teacher profile, updates user role")
    void performRoleChange_teacherToStudent_createsStudent_softDeletesTeacher_updatesRole() {
        var teacher = activeTeacher("role.teacher.to.student.create@example.com");
        var group = activeGroup("RC-201");
        var form = toStudentForm(group.getId(), ENROLLMENT_YEAR);

        commitSetupBeforeRequiresNewServiceCall();

        roleChangeService.performRoleChange(
                teacher.getId(),
                UserRole.TEACHER,
                studentTargetHandler,
                form
        );

        assertUserRole(teacher.getId(), UserRole.STUDENT);

        assertTrue(teacherRepository.findById(teacher.getId()).isEmpty());
        assertTrue(studentRepository.findById(teacher.getId()).isPresent());

        assertProfileDeleted("teacher", teacher.getId());
        assertProfileActive("student", teacher.getId());

        var student = studentRepository.findById(teacher.getId()).orElseThrow();
        assertEquals(group.getId(), student.getGroup().getId());
        assertEquals(ENROLLMENT_YEAR, student.getEnrollmentYear());
    }

    @Test
    @DisplayName("performRoleChange: teacher -> student restores deleted student profile when previous group is active")
    void performRoleChange_teacherToStudent_restoresDeletedStudentWithActiveGroup_updatesRole() {
        var teacher = activeTeacher("role.teacher.to.student.restore@example.com");
        var group = activeGroup("RC-202");

        insertDeletedStudentProfile(teacher.getId(), group.getId(), ENROLLMENT_YEAR);

        commitSetupBeforeRequiresNewServiceCall();

        roleChangeService.performRoleChange(
                teacher.getId(),
                UserRole.TEACHER,
                studentTargetHandler,
                null
        );

        assertUserRole(teacher.getId(), UserRole.STUDENT);

        assertTrue(teacherRepository.findById(teacher.getId()).isEmpty());
        assertTrue(studentRepository.findById(teacher.getId()).isPresent());

        assertProfileDeleted("teacher", teacher.getId());
        assertProfileActive("student", teacher.getId());

        var student = studentRepository.findById(teacher.getId()).orElseThrow();
        assertEquals(group.getId(), student.getGroup().getId());
        assertEquals(ENROLLMENT_YEAR, student.getEnrollmentYear());
    }

    @Test
    @DisplayName("performRoleChange: teacher -> student with archived previous group -> RoleChangeException")
    void performRoleChange_teacherToStudent_archivedPreviousGroup_fails() {
        var teacher = activeTeacher("role.teacher.to.student.archived.group@example.com");
        var group = activeGroup("RC-203");

        insertDeletedStudentProfile(teacher.getId(), group.getId(), ENROLLMENT_YEAR);
        softDeleteGroup(group.getId());

        commitSetupBeforeRequiresNewServiceCall();

        assertThrows(RoleChangeException.class,
                () -> roleChangeService.performRoleChange(
                        teacher.getId(),
                        UserRole.TEACHER,
                        studentTargetHandler,
                        null
                ));
    }

    @Test
    @DisplayName("performRoleChange: teacher -> student with assigned courses -> rollback, teacher remains active")
    void performRoleChange_teacherToStudent_teacherWithCourses_failsAndRollsBack() {
        var teacher = activeTeacher("role.teacher.with.course@example.com");
        var group = activeGroup("RC-204");
        var form = toStudentForm(group.getId(), ENROLLMENT_YEAR);

        initializer.persistAll(Course.builder()
                .code(uniqueValue("RC-COURSE-204"))
                .name("Role Change Locked Course")
                .description(null)
                .teacher(teacher)
                .build());

        commitSetupBeforeRequiresNewServiceCall();

        assertThrows(RoleChangeException.class,
                () -> roleChangeService.performRoleChange(
                        teacher.getId(),
                        UserRole.TEACHER,
                        studentTargetHandler,
                        form
                ));

        assertUserRole(teacher.getId(), UserRole.TEACHER);

        assertTrue(teacherRepository.findById(teacher.getId()).isPresent());
        assertTrue(studentRepository.findById(teacher.getId()).isEmpty());

        assertProfileActive("teacher", teacher.getId());
    }

    @Test
    @DisplayName("performRoleChange: source role mismatch -> RoleChangeException")
    void performRoleChange_sourceRoleMismatch_fails() {
        var student = activeStudent("role.student.mismatch.source@example.com", "RC-301");
        var form = toTeacherForm(AcademicRank.LECTURER, OFFICE_A);

        commitSetupBeforeRequiresNewServiceCall();

        assertThrows(RoleChangeException.class,
                () -> roleChangeService.performRoleChange(
                        student.getId(),
                        UserRole.TEACHER,
                        teacherTargetHandler,
                        form
                ));

        assertUserRole(student.getId(), UserRole.STUDENT);
        assertTrue(studentRepository.findById(student.getId()).isPresent());
        assertTrue(teacherRepository.findById(student.getId()).isEmpty());
    }

    @Test
    @DisplayName("performRoleChange: target role same as current role -> RoleChangeException")
    void performRoleChange_sameTargetRole_fails() {
        var student = activeStudent("role.student.same.target@example.com", "RC-303");
        var group = activeGroup("RC-304");
        var form = toStudentForm(group.getId(), ENROLLMENT_YEAR);

        commitSetupBeforeRequiresNewServiceCall();

        assertThrows(RoleChangeException.class,
                () -> roleChangeService.performRoleChange(
                        student.getId(),
                        UserRole.STUDENT,
                        studentTargetHandler,
                        form
                ));

        assertUserRole(student.getId(), UserRole.STUDENT);
        assertTrue(studentRepository.findById(student.getId()).isPresent());
        assertTrue(teacherRepository.findById(student.getId()).isEmpty());
    }

    private ToTeacherRoleChangeDto toTeacherForm(AcademicRank rank, String office) {
        return new ToTeacherRoleChangeDto(rank, office);
    }

    private ToStudentRoleChangeDto toStudentForm(Long groupId, Integer enrollmentYear) {
        return new ToStudentRoleChangeDto(groupId, enrollmentYear);
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
                .office(OFFICE_A)
                .build()).getFirst();
    }

    private AppUser activeUser(String email, UserRole role) {
        return initializer.persistAll(AppUser.builder()
                .email(uniqueEmail(email))
                .password(PASSWORD)
                .role(role)
                .firstName(FIRST_NAME)
                .lastName(LAST_NAME)
                .enabled(true)
                .build()).getFirst();
    }

    private StudyGroup activeGroup(String name) {
        return initializer.persistAll(StudyGroup.builder()
                .name(uniqueValue(name))
                .build()).getFirst();
    }

    private void insertDeletedTeacherProfile(long userId, AcademicRank rank, String office) {
        em.createNativeQuery("""
                insert into teacher (id, academic_rank, office, deleted_at)
                values (:id, :rank, :office, now())
                """)
                .setParameter("id", userId)
                .setParameter("rank", rank.name())
                .setParameter("office", office)
                .executeUpdate();

        em.flush();
        em.clear();
    }

    private void insertDeletedStudentProfile(long userId, long groupId, Integer enrollmentYear) {
        em.createNativeQuery("""
                insert into student (id, group_id, enrollment_year, deleted_at)
                values (:id, :groupId, :enrollmentYear, now())
                """)
                .setParameter("id", userId)
                .setParameter("groupId", groupId)
                .setParameter("enrollmentYear", enrollmentYear)
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

    // RoleChangeService uses REQUIRES_NEW, so setup data must be committed
    // before calling the service; otherwise the new transaction may not see it.
    private void commitSetupBeforeRequiresNewServiceCall() {
        assertTrue(TestTransaction.isActive(), "Test transaction must be active before setup commit");

        em.flush();
        em.clear();

        TestTransaction.flagForCommit();
        TestTransaction.end();
    }

    private void assertUserRole(long userId, UserRole expectedRole) {
        em.clear();

        var user = userRepository.findById(userId).orElseThrow();
        assertEquals(expectedRole, user.getRole());
    }

    private void assertProfileActive(String tableName, long id) {
        em.clear();

        var deletedAt = em.createNativeQuery("""
                select deleted_at
                from %s
                where id = :id
                """.formatted(tableName))
                .setParameter("id", id)
                .getSingleResult();

        assertNull(deletedAt, tableName + " profile should be active");
    }

    private void assertProfileDeleted(String tableName, long id) {
        em.clear();

        var deletedAt = em.createNativeQuery("""
                select deleted_at
                from %s
                where id = :id
                """.formatted(tableName))
                .setParameter("id", id)
                .getSingleResult();

        assertNotNull(deletedAt, tableName + " profile should be soft-deleted");
    }

    private String uniqueEmail(String email) {
        var at = email.indexOf('@');

        if (at < 0) {
            return uniqueValue(email);
        }

        return email.substring(0, at) + "." + System.nanoTime() + email.substring(at);
    }

    private String uniqueValue(String value) {
        return value + "-" + System.nanoTime();
    }
}