package ua.foxminded.university.model.persistence.teacher;

import static org.junit.jupiter.api.Assertions.*;
import static ua.foxminded.university.model.persistence.teacher.TeacherRepositoryTestConstants.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import ua.foxminded.university.TestcontainersConfiguration;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
@Import(TestcontainersConfiguration.class)
@Sql(
        scripts = "/sql/teacher/teacher-repository-base.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
class TeacherRepositoryDirectoryCardsTest {

    @Autowired
    private TeacherRepository teacherRepository;

    @Test
    @DisplayName("findTeacherCardsForAdmin: returns enabled active teachers")
    void findTeacherCardsForAdmin_returnsEnabledActiveTeachers() {
        var page = teacherRepository.findTeacherCardsForAdmin(
                PageRequest.of(
                        0,
                        10));

        assertEquals(
                ACTIVE_ENABLED_TEACHER_COUNT,
                page.getTotalElements());

        assertEquals(
                List.of(
                        TEACHER_2_ID,
                        TEACHER_3_ID,
                        TEACHER_1_ID),
                page.getContent()
                        .stream()
                        .map(card -> card.id())
                        .toList());

        assertFalse(
                page.getContent()
                        .stream()
                        .anyMatch(card ->
                                card.id().equals(
                                        TEACHER_BLOCKED_ID)));

        assertFalse(
                page.getContent()
                        .stream()
                        .anyMatch(card ->
                                card.id().equals(
                                        TEACHER_DELETED_ID)));

        var card = page.getContent()
                .stream()
                .filter(teacher ->
                        teacher.id().equals(
                                TEACHER_1_ID))
                .findFirst()
                .orElseThrow();

        assertAll(
                () -> assertEquals(
                        TEACHER_1_EMAIL,
                        card.email()),
                () -> assertEquals(
                        TEACHER_1_FIRST_NAME,
                        card.firstName()),
                () -> assertEquals(
                        TEACHER_1_LAST_NAME,
                        card.lastName()),
                () -> assertEquals(
                        TEACHER_1_RANK,
                        card.academicRank()),
                () -> assertEquals(
                        TEACHER_1_OFFICE,
                        card.office()));
    }

    @Test
    @DisplayName("findTeacherCardsForAdmin: pageable returns correct totals")
    void findTeacherCardsForAdmin_pageable_returnsCorrectTotals() {
        var page = teacherRepository.findTeacherCardsForAdmin(
                PageRequest.of(
                        0,
                        2));

        assertEquals(
                2,
                page.getContent().size());

        assertEquals(
                ACTIVE_ENABLED_TEACHER_COUNT,
                page.getTotalElements());

        assertEquals(
                2,
                page.getTotalPages());

        assertTrue(
                page.hasNext());
    }

    @Test
    @DisplayName("findTeacherCardsForTeacher: returns enabled colleagues excluding current teacher")
    void findTeacherCardsForTeacher_returnsEnabledColleaguesExcludingSelf() {
        var page = teacherRepository.findTeacherCardsForTeacher(
                TEACHER_1_ID,
                PageRequest.of(
                        0,
                        10));

        assertEquals(
                2L,
                page.getTotalElements());

        assertEquals(
                List.of(
                        TEACHER_2_ID,
                        TEACHER_3_ID),
                page.getContent()
                        .stream()
                        .map(card -> card.id())
                        .toList());

        assertFalse(
                page.getContent()
                        .stream()
                        .anyMatch(card ->
                                card.id().equals(
                                        TEACHER_1_ID)));

        assertFalse(
                page.getContent()
                        .stream()
                        .anyMatch(card ->
                                card.id().equals(
                                        TEACHER_BLOCKED_ID)));
    }

    @Test
    @DisplayName("findTeacherCardsForTeacher: missing teacher id returns all enabled teachers")
    void findTeacherCardsForTeacher_missingTeacher_returnsEnabledTeachers() {
        var page = teacherRepository.findTeacherCardsForTeacher(
                MISSING_ID,
                PageRequest.of(
                        0,
                        10));

        assertEquals(
                ACTIVE_ENABLED_TEACHER_COUNT,
                page.getTotalElements());

        assertEquals(
                List.of(
                        TEACHER_2_ID,
                        TEACHER_3_ID,
                        TEACHER_1_ID),
                page.getContent()
                        .stream()
                        .map(card -> card.id())
                        .toList());
    }

    @Test
    @DisplayName("findTeacherCardsForStudent: returns enabled group teachers without duplicates")
    void findTeacherCardsForStudent_returnsGroupTeachersWithoutDuplicates() {
        var page = teacherRepository.findTeacherCardsForStudent(
                STUDENT_CS_ID,
                PageRequest.of(
                        0,
                        10));

        assertEquals(
                1L,
                page.getTotalElements(),
                "multiple courses taught by one teacher must not duplicate teacher cards");

        var card = page.getContent()
                .getFirst();

        assertAll(
                () -> assertEquals(
                        TEACHER_1_ID,
                        card.id()),
                () -> assertEquals(
                        TEACHER_1_EMAIL,
                        card.email()),
                () -> assertEquals(
                        TEACHER_1_RANK,
                        card.academicRank()),
                () -> assertEquals(
                        TEACHER_1_OFFICE,
                        card.office()));

        assertFalse(
                page.getContent()
                        .stream()
                        .anyMatch(teacher ->
                                teacher.id().equals(
                                        TEACHER_BLOCKED_ID)));
    }

    @Test
    @DisplayName("findTeacherCardsForStudent: second student returns teacher from own group")
    void findTeacherCardsForStudent_secondStudent_returnsOwnTeacher() {
        var page = teacherRepository.findTeacherCardsForStudent(
                STUDENT_EE_ID,
                PageRequest.of(
                        0,
                        10));

        assertEquals(
                1L,
                page.getTotalElements());

        assertEquals(
                TEACHER_2_ID,
                page.getContent()
                        .getFirst()
                        .id());
    }

    @Test
    @DisplayName("findTeacherCardsForStudent: missing student -> empty page")
    void findTeacherCardsForStudent_missingStudent_returnsEmptyPage() {
        var page = teacherRepository.findTeacherCardsForStudent(
                MISSING_ID,
                PageRequest.of(
                        0,
                        10));

        assertTrue(
                page.isEmpty());

        assertEquals(
                0L,
                page.getTotalElements());
    }
}