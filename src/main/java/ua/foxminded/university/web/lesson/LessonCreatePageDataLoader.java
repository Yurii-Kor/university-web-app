package ua.foxminded.university.web.lesson;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

import lombok.RequiredArgsConstructor;
import ua.foxminded.university.model.domain.enums.LessonType;
import ua.foxminded.university.model.persistence.course.projection.CourseCardView;
import ua.foxminded.university.service.course.CourseService;
import ua.foxminded.university.service.course.dto.CourseGroupsView;
import ua.foxminded.university.service.lesson.LessonService;
import ua.foxminded.university.service.lesson.dto.LessonCreateDto;

@Component
@RequiredArgsConstructor
public class LessonCreatePageDataLoader {

    private final CourseService courseService;
    private final LessonService lessonService;
    private final Clock clock;

    public void addPageData(
            Model model,
            long teacherId,
            LessonCreateDto form) {

        var selectedDate = Optional.ofNullable(form.date())
                .orElseGet(this::today);

        var dayFrom = startOfDay(selectedDate);
        var dayTo = startOfDay(selectedDate.plusDays(1));
        var courses = loadTeacherCourses(teacherId);

        addCommonData(model, courses);
        addTeacherSchedule(model, teacherId, dayFrom, dayTo);
        resetSelectionData(model);
        addSelectedCourseData(
                model,
                courses,
                form.courseId(),
                form.groupId(),
                dayFrom,
                dayTo);
    }

    private List<CourseCardView> loadTeacherCourses(long teacherId) {
        return courseService.listCourseCardsForTeacher(
                teacherId,
                Pageable.unpaged())
                .getContent();
    }

    private void addCommonData(
            Model model,
            List<CourseCardView> courses) {

        model.addAttribute("all_courses", courses);
        model.addAttribute("lesson_types", LessonType.values());
        model.addAttribute("today", today());
    }

    private void addTeacherSchedule(
            Model model,
            long teacherId,
            OffsetDateTime dayFrom,
            OffsetDateTime dayTo) {

        model.addAttribute(
                "teacher_schedule",
                lessonService.listTeacherScheduleItems(
                        teacherId,
                        dayFrom,
                        dayTo));
    }

    private void resetSelectionData(Model model) {
        model.addAttribute("course_groups", null);
        model.addAttribute("selected_course", null);
        model.addAttribute("selected_group_valid", false);
        model.addAttribute("group_schedule", null);
    }

    private void addSelectedCourseData(
            Model model,
            List<CourseCardView> courses,
            Long selectedCourseId,
            Long selectedGroupId,
            OffsetDateTime dayFrom,
            OffsetDateTime dayTo) {

        var selectedCourse = findSelectedCourse(
                courses,
                selectedCourseId);

        if (selectedCourse.isEmpty()) {
            return;
        }

        model.addAttribute(
                "selected_course",
                selectedCourse.get());

        var courseGroups =
                courseService.getCourseGroupsView(
                        selectedCourseId);

        model.addAttribute(
                "course_groups",
                courseGroups);

        addSelectedGroupSchedule(
                model,
                courseGroups,
                selectedGroupId,
                dayFrom,
                dayTo);
    }

    private Optional<CourseCardView> findSelectedCourse(
            List<CourseCardView> courses,
            Long selectedCourseId) {

        if (selectedCourseId == null) {
            return Optional.empty();
        }

        return courses.stream()
                .filter(course ->
                        Objects.equals(
                                course.id(),
                                selectedCourseId))
                .findFirst();
    }

    private void addSelectedGroupSchedule(
            Model model,
            CourseGroupsView courseGroups,
            Long selectedGroupId,
            OffsetDateTime dayFrom,
            OffsetDateTime dayTo) {

        if (!isAssignedGroup(
                courseGroups,
                selectedGroupId)) {
            return;
        }

        model.addAttribute(
                "selected_group_valid",
                true);

        model.addAttribute(
                "group_schedule",
                lessonService.listGroupScheduleItems(
                        selectedGroupId,
                        dayFrom,
                        dayTo));
    }

    private boolean isAssignedGroup(
            CourseGroupsView courseGroups,
            Long selectedGroupId) {

        if (selectedGroupId == null) {
            return false;
        }

        return courseGroups.assigned()
                .stream()
                .anyMatch(group ->
                        Objects.equals(
                                group.id(),
                                selectedGroupId));
    }

    private OffsetDateTime startOfDay(LocalDate date) {
        return date.atStartOfDay(
                clock.getZone())
                .toOffsetDateTime();
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }
}