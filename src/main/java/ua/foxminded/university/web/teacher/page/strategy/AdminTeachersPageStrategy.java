package ua.foxminded.university.web.teacher.page.strategy;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import ua.foxminded.university.model.persistence.teacher.projection.TeacherDirectoryCardView;
import ua.foxminded.university.service.teacher.TeacherService;
import ua.foxminded.university.web.teacher.page.TeachersPageMode;
import ua.foxminded.university.web.teacher.page.TeachersPageStrategy;

@Component
@RequiredArgsConstructor
public class AdminTeachersPageStrategy implements TeachersPageStrategy {

    private final TeacherService teacherService;

    @Override
    public TeachersPageMode mode() {

        return TeachersPageMode.ADMIN;
    }

    @Override
    public Page<TeacherDirectoryCardView> loadTeachers(
            long userId,
            Pageable pageable) {

        return teacherService.listTeacherCardsForAdmin(pageable);
    }
}