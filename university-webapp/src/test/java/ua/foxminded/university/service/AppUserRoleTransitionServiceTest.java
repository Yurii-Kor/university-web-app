package ua.foxminded.university.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import jakarta.validation.ConstraintViolationException;
import ua.foxminded.university.TestcontainersConfiguration;
import ua.foxminded.university.model.domain.AppUser;
import ua.foxminded.university.model.domain.Course;
import ua.foxminded.university.model.domain.Student;
import ua.foxminded.university.model.domain.StudyGroup;
import ua.foxminded.university.model.domain.Teacher;
import ua.foxminded.university.model.domain.enums.AcademicRank;
import ua.foxminded.university.model.domain.enums.UserRole;
import ua.foxminded.university.model.repository.AppUserRepository;
import ua.foxminded.university.model.repository.CourseRepository;
import ua.foxminded.university.model.repository.StudentRepository;
import ua.foxminded.university.model.repository.StudyGroupRepository;
import ua.foxminded.university.model.repository.TeacherRepository;
import ua.foxminded.university.service.dto.request.appuser.StudentToTeacherRoleChangeDto;
import ua.foxminded.university.service.dto.request.appuser.TeacherToStudentRoleChangeDto;
import ua.foxminded.university.service.exception.appuser.StudentToTeacherRoleChangeException;
import ua.foxminded.university.service.exception.appuser.TeacherToStudentRoleChangeException;
import ua.foxminded.university.service.util.validation.EntityValidatior;
import ua.foxminded.university.service.util.validation.config.ValidatorConfig;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Import({
    TestcontainersConfiguration.class,
    AppUserRoleTransitionService.class,
    ValidatorConfig.class,
    EntityValidatior.class
})
class AppUserRoleTransitionServiceTest {

    private static final String PWD = "Abcd1234!";
    private static final Long MISSING_ID = 999_999L;

    @Autowired
    private AppUserRoleTransitionService roleTransitionService;

    @Autowired
    private AppUserRepository usersRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private StudyGroupRepository groupRepository;

    @Autowired
    private CourseRepository courseRepository;

    private StudyGroup roleChangeGroup;

    private Student studentToTeacher;
    private Student testStudent;

    private Teacher teacherToStudent;
    private Teacher teacherWithCourses;
    
    private AcademicRank anyRank() {
        return AcademicRank.values()[0];
    }

    @BeforeAll
    void setup() {
        roleChangeGroup = groupRepository.save(
                StudyGroup.builder()
                        .name("QA-11")
                        .build()
        );

        studentToTeacher = studentRepository.save(
                Student.builder()
                        .user(AppUser.builder()
                                .email("student.to.teacher@example.com")
                                .password(PWD)
                                .role(UserRole.STUDENT)
                                .firstName("Role")
                                .lastName("Student")
                                .enabled(true)
                                .build())
                        .group(roleChangeGroup)
                        .enrollmentYear(2024)
                        .build()
        );

        testStudent = studentRepository.save(
                Student.builder()
                        .user(AppUser.builder()
                                .email("student.negative@example.com")
                                .password(PWD)
                                .role(UserRole.STUDENT)
                                .firstName("Negative")
                                .lastName("Student")
                                .enabled(true)
                                .build())
                        .group(roleChangeGroup)
                        .enrollmentYear(2023)
                        .build()
        );

        teacherToStudent = teacherRepository.save(
                Teacher.builder()
                        .user(AppUser.builder()
                                .email("teacher.to.student@example.com")
                                .password(PWD)
                                .role(UserRole.TEACHER)
                                .firstName("Role")
                                .lastName("Teacher")
                                .enabled(true)
                                .build())
                        .academicRank(anyRank())
                        .office("T-101")
                        .build()
        );

        teacherWithCourses = teacherRepository.save(
                Teacher.builder()
                        .user(AppUser.builder()
                                .email("teacher.with.course@example.com")
                                .password(PWD)
                                .role(UserRole.TEACHER)
                                .firstName("Busy")
                                .lastName("Teacher")
                                .enabled(true)
                                .build())
                        .academicRank(anyRank())
                        .office("T-202")
                        .build()
        );

        courseRepository.save(
                Course.builder()
                        .code("NEG-COURSE-01")
                        .name("Negative Test Course")
                        .description("Course for role transition negative case")
                        .teacher(teacherWithCourses)
                        .build()
        );
    }

    @Test
    @DisplayName("changeStudentToTeacher: happy path — replaces student profile with teacher profile and updates role")
    void changeStudentToTeacher_happyPath_success() {
        var form = new StudentToTeacherRoleChangeDto(
                studentToTeacher.getId(),
                anyRank(),
                "B-101"
        );

        roleTransitionService.changeStudentToTeacher(form);

        var reloadedUser = usersRepository.findById(studentToTeacher.getId()).orElseThrow();
        assertEquals(UserRole.TEACHER, reloadedUser.getRole());

        assertTrue(studentRepository.findById(studentToTeacher.getId()).isEmpty(),
                "student profile must be removed");

        var teacher = teacherRepository.findById(studentToTeacher.getId()).orElseThrow();
        assertEquals("B-101", teacher.getOffice());
        assertEquals(anyRank(), teacher.getAcademicRank());
    }

    @Test
    @DisplayName("changeStudentToTeacher: missing user -> StudentToTeacherRoleChangeException")
    void changeStudentToTeacher_missingUser_fails() {
        var form = new StudentToTeacherRoleChangeDto(
                MISSING_ID,
                anyRank(),
                "B-102"
        );

        assertThrows(StudentToTeacherRoleChangeException.class,
                () -> roleTransitionService.changeStudentToTeacher(form));
    }

    @Test
    @DisplayName("changeStudentToTeacher: wrong source role (teacher) -> StudentToTeacherRoleChangeException")
    void changeStudentToTeacher_wrongSourceRole_fails() {
        var form = new StudentToTeacherRoleChangeDto(
                teacherWithCourses.getId(),
                anyRank(),
                "B-103"
        );

        assertThrows(StudentToTeacherRoleChangeException.class,
                () -> roleTransitionService.changeStudentToTeacher(form));
    }

    @Test
    @DisplayName("changeStudentToTeacher: invalid dto -> ConstraintViolationException")
    void changeStudentToTeacher_invalidDto_fails() {
        var bad = new StudentToTeacherRoleChangeDto(
                null,
                null,
                ""
        );

        assertThrows(ConstraintViolationException.class,
                () -> roleTransitionService.changeStudentToTeacher(bad));
    }

    @Test
    @DisplayName("changeTeacherToStudent: happy path — replaces teacher profile with student profile and updates role")
    void changeTeacherToStudent_happyPath_success() {
        var form = new TeacherToStudentRoleChangeDto(
                teacherToStudent.getId(),
                roleChangeGroup.getId(),
                2022
        );

        roleTransitionService.changeTeacherToStudent(form);

        var reloadedUser = usersRepository.findById(teacherToStudent.getId()).orElseThrow();
        assertEquals(UserRole.STUDENT, reloadedUser.getRole());

        assertTrue(teacherRepository.findById(teacherToStudent.getId()).isEmpty(),
                "teacher profile must be removed");

        var student = studentRepository.findById(teacherToStudent.getId()).orElseThrow();
        assertEquals(roleChangeGroup.getId(), student.getGroup().getId());
        assertEquals(2022, student.getEnrollmentYear());
    }

    @Test
    @DisplayName("changeTeacherToStudent: missing user -> TeacherToStudentRoleChangeException")
    void changeTeacherToStudent_missingUser_fails() {
        var form = new TeacherToStudentRoleChangeDto(
                MISSING_ID,
                roleChangeGroup.getId(),
                2022
        );

        assertThrows(TeacherToStudentRoleChangeException.class,
                () -> roleTransitionService.changeTeacherToStudent(form));
    }

    @Test
    @DisplayName("changeTeacherToStudent: wrong source role (student) -> TeacherToStudentRoleChangeException")
    void changeTeacherToStudent_wrongSourceRole_fails() {
        var form = new TeacherToStudentRoleChangeDto(
                testStudent.getId(),
                roleChangeGroup.getId(),
                2022
        );

        assertThrows(TeacherToStudentRoleChangeException.class,
                () -> roleTransitionService.changeTeacherToStudent(form));
    }

    @Test
    @DisplayName("changeTeacherToStudent: teacher has assigned courses -> TeacherToStudentRoleChangeException")
    void changeTeacherToStudent_teacherHasCourses_fails() {
        var form = new TeacherToStudentRoleChangeDto(
                teacherWithCourses.getId(),
                roleChangeGroup.getId(),
                2022
        );

        assertThrows(TeacherToStudentRoleChangeException.class,
                () -> roleTransitionService.changeTeacherToStudent(form));
    }

    @Test
    @DisplayName("changeTeacherToStudent: target group not found -> TeacherToStudentRoleChangeException")
    void changeTeacherToStudent_missingGroup_fails() {
        var form = new TeacherToStudentRoleChangeDto(
                teacherWithCourses.getId(),
                MISSING_ID,
                2022
        );

        assertThrows(TeacherToStudentRoleChangeException.class,
                () -> roleTransitionService.changeTeacherToStudent(form));
    }

    @Test
    @DisplayName("changeTeacherToStudent: invalid dto -> ConstraintViolationException")
    void changeTeacherToStudent_invalidDto_fails() {
        var bad = new TeacherToStudentRoleChangeDto(
                null,
                null,
                null
        );

        assertThrows(ConstraintViolationException.class,
                () -> roleTransitionService.changeTeacherToStudent(bad));
    }
}