package ua.foxminded.university.service.lesson;

import java.time.OffsetDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import lombok.RequiredArgsConstructor;
import ua.foxminded.university.model.domain.Course;
import ua.foxminded.university.model.domain.Lesson;
import ua.foxminded.university.model.domain.StudyGroup;
import ua.foxminded.university.model.persistence.course.CourseRepository;
import ua.foxminded.university.model.persistence.lesson.LessonRepository;
import ua.foxminded.university.model.persistence.lesson.projection.LessonScheduleItemView;
import ua.foxminded.university.model.persistence.studygroup.StudyGroupRepository;
import ua.foxminded.university.service.lesson.dto.LessonCreateDto;
import ua.foxminded.university.service.lesson.dto.LessonUpdateDto;
import ua.foxminded.university.service.lesson.exception.LessonCreateException;
import ua.foxminded.university.service.lesson.exception.LessonUpdateException;
import ua.foxminded.university.service.util.DtoMapper;

@Service
@RequiredArgsConstructor
@Transactional
public class LessonService {

	private static final Logger log =
			LoggerFactory.getLogger(LessonService.class);

	private final LessonRepository lessonRepository;
	private final CourseRepository courseRepository;
	private final StudyGroupRepository groupRepository;

	private final LessonPolicy lessonPolicy;
	private final LessonConflictChecker conflictChecker;
	private final DtoMapper dtoMapper;

	@Transactional(value = TxType.REQUIRES_NEW)
	public Lesson create(
			long teacherId,
			LessonCreateDto draft) {

		var course =
				requireCourse(draft.courseId());

		var group =
				requireGroup(draft.groupId());

		var lesson =
				dtoMapper.toLessonEntity(
						draft,
						course,
						group);

		var violations =
				lessonPolicy.validateForCreate(
						lesson,
						teacherId);

		if (!violations.isEmpty()) {
			throw new LessonCreateException(
					draft,
					violations);
		}

		conflictChecker.assertNoConflicts(
				lesson,
				teacherId);

		return lessonRepository.save(lesson);
	}

	@Transactional(value = TxType.REQUIRES_NEW)
	public Lesson update(
			long teacherId,
			long lessonId,
			LessonUpdateDto form) {

		var lesson =
				requireLesson(lessonId);

		var violations =
				lessonPolicy.validateCanUpdate(
						lesson,
						teacherId);

		if (!violations.isEmpty()) {
			throw new LessonUpdateException(
					lessonId,
					form,
					violations);
		}

		dtoMapper.applyLessonUpdate(
				lesson,
				form);

		violations =
				lessonPolicy.validateAfterUpdate(
						lesson,
						teacherId);

		if (!violations.isEmpty()) {
			throw new LessonUpdateException(
					lessonId,
					form,
					violations);
		}

		conflictChecker.assertNoConflicts(
				lesson,
				teacherId,
				lessonId);

		log.info(
				"Updated lesson: id={}",
				lessonId);

		return lesson;
	}

	@Transactional(value = TxType.REQUIRES_NEW)
	public void deleteById(
			long teacherId,
			long lessonId) {

		var lesson =
				requireLesson(lessonId);

		var violations =
				lessonPolicy.validateCanModify(
						lesson,
						teacherId);

		if (!violations.isEmpty()) {
			throw new IllegalStateException(
					violations.getFirst()
							.message());
		}

		lessonRepository.delete(lesson);

		log.info(
				"Deleted lesson: id={}",
				lessonId);
	}

	@Transactional(value = TxType.SUPPORTS)
	public List<Lesson> listGroupSchedule(
			long groupId,
			OffsetDateTime from,
			OffsetDateTime to) {

		return lessonRepository.findGroupSchedule(
				groupId,
				from,
				to);
	}

	@Transactional(value = TxType.SUPPORTS)
	public List<Lesson> listTeacherSchedule(
			long teacherId,
			OffsetDateTime from,
			OffsetDateTime to) {

		return lessonRepository.findTeacherSchedule(
				teacherId,
				from,
				to);
	}

	@Transactional(value = TxType.SUPPORTS)
	public List<LessonScheduleItemView> listGroupScheduleItems(
			long groupId,
			OffsetDateTime from,
			OffsetDateTime to) {

		return lessonRepository.findGroupScheduleItems(
				groupId,
				from,
				to);
	}

	@Transactional(value = TxType.SUPPORTS)
	public List<LessonScheduleItemView> listTeacherScheduleItems(
			long teacherId,
			OffsetDateTime from,
			OffsetDateTime to) {

		return lessonRepository.findTeacherScheduleItems(
				teacherId,
				from,
				to);
	}

	private Course requireCourse(long courseId) {
		return courseRepository.findById(courseId)
				.orElseThrow(() ->
						new EntityNotFoundException(
								"Course not found: id="
										+ courseId));
	}

	private StudyGroup requireGroup(long groupId) {
		return groupRepository.findById(groupId)
				.orElseThrow(() ->
						new EntityNotFoundException(
								"StudyGroup not found: id="
										+ groupId));
	}

	private Lesson requireLesson(long lessonId) {
		return lessonRepository.findById(lessonId)
				.orElseThrow(() ->
						new EntityNotFoundException(
								"Lesson not found: id="
										+ lessonId));
	}
}