package ua.foxminded.university.web.student.cards.strategy;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import ua.foxminded.university.model.persistence.student.projection.TeacherStudentCardView;
import ua.foxminded.university.service.student.StudentService;
import ua.foxminded.university.web.student.cards.StudentsCardsPageMode;
import ua.foxminded.university.web.student.cards.StudentsCardsPageStrategy;

@Component
@RequiredArgsConstructor
public class TeacherStudentsCardsPageStrategy
        implements StudentsCardsPageStrategy<TeacherStudentCardView> {

    private final StudentService studentService;

    @Override
    public StudentsCardsPageMode mode() {
        return StudentsCardsPageMode.TEACHER;
    }

    @Override
    public Page<TeacherStudentCardView> loadStudents(
            long userId,
            Pageable pageable) {

        return studentService.listStudentCardsForTeacher(
                userId,
                pageable);
    }
}
