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
class TeacherRepositoryRoleChangeCardsTest {

    @Autowired
    private TeacherRepository teacherRepository;

    @Test
    @DisplayName("findTeacherRoleChangeCardsAll: returns active teachers including disabled")
    void findTeacherRoleChangeCardsAll_returnsActiveTeachersIncludingDisabled() {
        var page = teacherRepository.findTeacherRoleChangeCardsAll(
                PageRequest.of(
                        0,
                        10));

        assertEquals(
                ACTIVE_TEACHER_COUNT,
                page.getTotalElements());

        assertEquals(
                List.of(
                        TEACHER_2_ID,
                        TEACHER_3_ID,
                        TEACHER_1_ID,
                        TEACHER_BLOCKED_ID),
                page.getContent()
                        .stream()
                        .map(card -> card.id())
                        .toList());

        assertFalse(
                page.getContent()
                        .stream()
                        .anyMatch(card ->
                                card.id().equals(
                                        TEACHER_DELETED_ID)));

        var blocked = page.getContent()
                .stream()
                .filter(card ->
                        card.id().equals(
                                TEACHER_BLOCKED_ID))
                .findFirst()
                .orElseThrow();

        assertFalse(
                blocked.enabled());

        assertNotNull(
                blocked.createdAt());
    }

    @Test
    @DisplayName("findTeacherRoleChangeCardsAll: pageable returns correct totals")
    void findTeacherRoleChangeCardsAll_pageable_returnsCorrectTotals() {
        var page = teacherRepository.findTeacherRoleChangeCardsAll(
                PageRequest.of(
                        0,
                        2));

        assertEquals(
                2,
                page.getContent().size());

        assertEquals(
                ACTIVE_TEACHER_COUNT,
                page.getTotalElements());

        assertEquals(
                2,
                page.getTotalPages());

        assertTrue(
                page.hasNext());
    }
}