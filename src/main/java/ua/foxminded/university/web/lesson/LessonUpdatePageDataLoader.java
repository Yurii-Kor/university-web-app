package ua.foxminded.university.web.lesson;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

import lombok.RequiredArgsConstructor;
import ua.foxminded.university.model.domain.enums.LessonType;
import ua.foxminded.university.model.persistence.lesson.projection.LessonScheduleItemView;
import ua.foxminded.university.service.lesson.LessonService;
import ua.foxminded.university.service.lesson.dto.LessonUpdateDto;

@Component
@RequiredArgsConstructor
public class LessonUpdatePageDataLoader {

    private final LessonService lessonService;
    private final Clock clock;

    public LessonUpdateDto toForm(
            LessonScheduleItemView lesson) {

        var startTime =
                lesson.startTime()
                        .atZoneSameInstant(
                                clock.getZone());

        var endTime =
                lesson.endTime()
                        .atZoneSameInstant(
                                clock.getZone());

        return new LessonUpdateDto(
                startTime.toLocalDate(),
                startTime.toLocalTime(),
                endTime.toLocalTime(),
                lesson.room(),
                lesson.lessonType(),
                lesson.description());
    }

    public void addPageData(
            Model model,
            long teacherId,
            long lessonId,
            LessonUpdateDto form) {

        var lesson =
                lessonService.getTeacherScheduleItem(
                        teacherId,
                        lessonId);

        addPageData(
                model,
                teacherId,
                lesson,
                form);
    }

    public void addPageData(
            Model model,
            long teacherId,
            LessonScheduleItemView lesson,
            LessonUpdateDto form) {

        var selectedDate =
                Optional.ofNullable(form.date())
                        .orElseGet(() ->
                                toLocalDate(
                                        lesson.startTime()));

        var dayFrom =
                startOfDay(selectedDate);

        var dayTo =
                startOfDay(
                        selectedDate.plusDays(1));

        model.addAttribute(
                "lesson",
                lesson);

        model.addAttribute(
                "lesson_types",
                LessonType.values());

        model.addAttribute(
                "today",
                LocalDate.now(clock));

        model.addAttribute(
                "selected_date",
                selectedDate);

        model.addAttribute(
                "lesson_date",
                toLocalDate(
                        lesson.startTime()));

        model.addAttribute(
                "teacher_schedule",
                excludeCurrentLesson(
                        lessonService.listTeacherScheduleItems(
                                teacherId,
                                dayFrom,
                                dayTo),
                        lesson.id()));

        model.addAttribute(
                "group_schedule",
                excludeCurrentLesson(
                        lessonService.listGroupScheduleItems(
                                lesson.groupId(),
                                dayFrom,
                                dayTo),
                        lesson.id()));
    }

    private List<LessonScheduleItemView> excludeCurrentLesson(
            List<LessonScheduleItemView> lessons,
            Long lessonId) {

        return lessons.stream()
                .filter(lesson ->
                        !lesson.id()
                                .equals(
                                        lessonId))
                .toList();
    }

    private OffsetDateTime startOfDay(
            LocalDate date) {

        return date.atStartOfDay(
                clock.getZone())
                .toOffsetDateTime();
    }

    private LocalDate toLocalDate(
            OffsetDateTime dateTime) {

        return dateTime.atZoneSameInstant(
                clock.getZone())
                .toLocalDate();
    }
}