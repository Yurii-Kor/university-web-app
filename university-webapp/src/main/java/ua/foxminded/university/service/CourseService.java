package ua.foxminded.university.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ua.foxminded.university.model.domain.Course;
import ua.foxminded.university.model.domain.StudyGroup;
import ua.foxminded.university.model.repository.CourseRepository;
import ua.foxminded.university.model.repository.StudyGroupRepository;
import ua.foxminded.university.model.repository.TeacherRepository;
import ua.foxminded.university.model.repository.dto.CourseCardView;
import ua.foxminded.university.model.repository.dto.GroupOptionView;
import ua.foxminded.university.service.dto.request.course.CourseCreateDto;
import ua.foxminded.university.service.dto.request.course.CourseDescriptionUpdateDto;
import ua.foxminded.university.service.dto.request.course.CourseSelfUpdateDto;
import ua.foxminded.university.service.dto.response.CourseGroupsPageView;
import ua.foxminded.university.service.dto.response.DeleteResult;
import ua.foxminded.university.service.util.validation.EntityValidatior;
import ua.foxminded.university.service.util.DtoMapper;
import ua.foxminded.university.service.util.DuplicateGuard;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CourseService {

	private static final int NOT_UPDATED = 0;

	private static final Logger log = LoggerFactory.getLogger(CourseService.class);

	private final CourseRepository courseRepository;
	private final TeacherRepository teacherRepository;
	private final StudyGroupRepository groupRepository;
	private final EntityValidatior validator;
	private final DtoMapper dtoMapper;
	private final DuplicateGuard duplicateGuard;

	@Transactional(value = TxType.REQUIRES_NEW)
	public List<Course> createAll(Collection<CourseCreateDto> drafts) {
		drafts = Optional.ofNullable(drafts).orElseGet(List::of).stream().filter(Objects::nonNull).toList();
		if (drafts.isEmpty()) {
			log.warn("createAll: nothing to persist (null/empty input)");
			return List.of();
		}

		validator.validateAll(drafts);

		var toPersist = dtoMapper.toCourseEntities(drafts);
		if (toPersist.isEmpty()) {
			log.warn("createAll: nothing to persist (null/empty input)");
			return List.of();
		}

		var codesLower = toPersist.stream().map(Course::getCode).map(String::trim).map(String::toLowerCase).toList();
		var namesLower = toPersist.stream().map(Course::getName).map(String::trim).map(String::toLowerCase).toList();

		duplicateGuard.assertNoDuplicates(codesLower, "normalized course codes");
		duplicateGuard.assertNoDuplicates(namesLower, "normalized course names");

		assertCodesFreeInDbForCreate(codesLower);
		assertNamesFreeInDb(namesLower);

		assertTeachersExist(toPersist);
		attachTeacherRefs(toPersist);

		return courseRepository.saveAll(toPersist);
	}

	@Transactional(value = TxType.SUPPORTS)
	public List<Course> findByIds(Collection<Long> ids) {
		var distinct = Optional.ofNullable(ids)
				.orElseGet(Collections::emptyList)
				.stream()
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());

		if (distinct.isEmpty()) {
			log.warn("findByIds: null/empty input or only nulls after filtering");
			return List.of();
		}

		return courseRepository.findAllById(distinct);
	}
	
	@Transactional(value = TxType.SUPPORTS)
	public List<CourseCardView> listCourseCardsForAdmin() {
	    return courseRepository.findCourseCardsAll();
	}

	@Transactional(value = TxType.SUPPORTS)
	public List<CourseCardView> listCourseCardsForTeacher(Long teacherId) {
		Optional.ofNullable(teacherId).orElseThrow(() -> new IllegalArgumentException("teacherId must not be null"));
		
		return courseRepository.findCourseCardsByTeacherId(teacherId);
	}
	
	@Transactional(value = TxType.SUPPORTS)
	public List<CourseCardView> listCourseCardsForStudent(Long studentId) {
		Optional.ofNullable(studentId).orElseThrow(() -> new IllegalArgumentException("studentId must not be null"));

	    return courseRepository.findCourseCardsByGroupId(studentId);
	}
	
	@Transactional(value = TxType.SUPPORTS)
	public CourseGroupsPageView getCourseGroupsPage(Long courseId) {
		Optional.ofNullable(courseId).orElseThrow(() -> new IllegalArgumentException("courseId must not be null"));

	    var header = courseRepository.findCourseHeaderById(courseId)
	            .orElseThrow(() -> new EntityNotFoundException("Course not found: id=" + courseId));

	    List<GroupOptionView> assigned = groupRepository.findAssignedGroupOptions(courseId);
	    List<GroupOptionView> available = groupRepository.findAvailableGroupOptions(courseId);

	    return new CourseGroupsPageView(header, assigned, available);
	}

	@Transactional(value = TxType.REQUIRES_NEW)
	public Course updateSelf(CourseSelfUpdateDto patch) {
		requireValidation(patch);
		var managed = getManagedCourse(patch.id(), "updateSelf");
		
		Optional.ofNullable(patch.code())
				.filter(newCode -> !newCode.isEmpty())
				.filter(newCode -> !newCode.equalsIgnoreCase(Optional.ofNullable(managed.getCode()).orElse("")))
				.ifPresent(newCode -> {
					assertCodeFreeInDbForSelfUpdate(patch.id(), newCode.trim().toLowerCase());
					managed.setCode(newCode);
				});

		Optional.ofNullable(patch.name())
				.filter(newName -> !newName.isEmpty())
				.filter(newName -> !newName.equalsIgnoreCase(Optional.ofNullable(managed.getName()).orElse("")))
				.ifPresent(newName -> {
					assertNamesFreeInDb(List.of(newName.trim().toLowerCase()));
					managed.setName(newName);
				});
		
		Optional.ofNullable(patch.teacherId())
				.ifPresent(newTeacherId -> {
					var sameTeacher = Optional.ofNullable(managed.getTeacher())
                            .map(t -> newTeacherId.equals(t.getId()))
                            .orElse(false);
					
					if (!sameTeacher) {
						assertTeacherExists(newTeacherId);
						managed.setTeacher(teacherRepository.getReferenceById(newTeacherId));
					}
				});

		log.debug("updateSelf: updated code/name/teacher for courseId={}", managed.getId());
		return managed;
	}
	
	@Transactional(value = TxType.REQUIRES_NEW)
	public Course updateDescription(CourseDescriptionUpdateDto patch) {
		requireValidation(patch);
		var managed = getManagedCourse(patch.id(), "updateDescription");

		Optional.ofNullable(patch.description())
				.map(String::trim)
				.ifPresent(desc -> managed.setDescription(desc.isEmpty() ? null : desc));

		log.debug("updateDescription: updated description for courseId={}", managed.getId());
		return managed;
	}

	@Transactional(value = TxType.REQUIRES_NEW)
	public int addGroupsToCourse(Long courseId, Collection<Long> groupIds) {
		Optional.ofNullable(courseId).orElseThrow(() -> {
			log.error("addGroupsToCourse: courseId is null");
			return new IllegalArgumentException("courseId must not be null");
		});

		var distinct = Optional.ofNullable(groupIds)
				.orElseGet(Collections::emptyList)
				.stream()
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());

		if (distinct.isEmpty()) {
			log.warn("addGroupsToCourse: null/empty groupIds or only nulls -> nothing to do");
			return NOT_UPDATED;
		}

		var existing = new HashSet<>(groupRepository.findExistingIds(distinct));
		var missing = distinct.stream().filter(id -> !existing.contains(id)).toList();
		if (!missing.isEmpty()) {
			log.error("addGroupsToCourse: StudyGroups not found: {}", missing);
			throw new EntityNotFoundException("StudyGroups not found: " + missing);
		}

		return attachExistingGroupsToCourse(courseId, existing);
	}

	private int attachExistingGroupsToCourse(Long courseId, Set<Long> existingGroupIds) {
		var course = courseRepository.findById(courseId).orElseThrow(() -> {
			log.error("addGroupsToCourse: course not found: id={}", courseId);
			return new EntityNotFoundException("Course not found: id=" + courseId);
		});

		var groupsSet = Optional.ofNullable(course.getGroups()).orElseGet(() -> {
			course.setGroups(new LinkedHashSet<>());
			return course.getGroups();
		});

		var already = groupsSet.stream().map(StudyGroup::getId).filter(Objects::nonNull).collect(Collectors.toSet());
		var toAddIds = existingGroupIds.stream().filter(id -> !already.contains(id)).collect(Collectors.toSet());

		if (toAddIds.isEmpty()) {
			log.info("addGroupsToCourse: nothing new to add for courseId={}", courseId);
			return NOT_UPDATED;
		}

		toAddIds.stream().map(groupRepository::getReferenceById).forEach(groupsSet::add);
		log.info("addGroupsToCourse: added {} group(s) to courseId={}", toAddIds.size(), courseId);
		return toAddIds.size();
	}

	@Transactional(value = TxType.REQUIRES_NEW)
	public int removeGroupsFromCourse(Long courseId, Collection<Long> groupIds) {
		Optional.ofNullable(courseId).orElseThrow(() -> {
			log.error("removeGroupsFromCourse: courseId is null");
			return new IllegalArgumentException("courseId must not be null");
		});

		var distinct = Optional.ofNullable(groupIds)
				.orElseGet(Collections::emptyList)
				.stream()
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());

		if (distinct.isEmpty()) {
			log.warn("removeGroupsFromCourse: null/empty groupIds or only nulls -> nothing to do");
			return NOT_UPDATED;
		}

		return doRemoveGroupsFromCourse(courseId, distinct);
	}

	private int doRemoveGroupsFromCourse(Long courseId, Collection<Long> groupIds) {
		var course = courseRepository.findById(courseId).orElseThrow(() -> {
			log.error("removeGroupsFromCourse: course not found: id={}", courseId);
			return new EntityNotFoundException("Course not found: id=" + courseId);
		});

		var groupsSet = course.getGroups();
		if (Optional.ofNullable(groupsSet).map(Collection::isEmpty).orElse(true)) {
			log.info("removeGroupsFromCourse: courseId={} has no groups -> nothing to remove", courseId);
			return NOT_UPDATED;
		}

		int before = groupsSet.size();
		groupsSet.removeIf(g -> groupIds.contains(g.getId()));
		int removed = before - groupsSet.size();

		log.info("removeGroupsFromCourse: removed {} group(s) from courseId={}", removed, courseId);
		return removed;
	}

	@Transactional(value = TxType.REQUIRES_NEW)
	public DeleteResult deleteByIds(Collection<Long> ids) {
		if (Optional.ofNullable(ids).map(Collection::isEmpty).orElse(true)) {
			log.warn("deleteByIds called with null/empty list");
			return new DeleteResult(Set.of(), Set.of());
		}

		var distinct = ids.stream().filter(Objects::nonNull).collect(Collectors.toSet());
		var existing = courseRepository.findAllById(distinct);

		var deletedIds = existing.stream().map(Course::getId).collect(Collectors.toSet());
		var notFound = distinct.stream().filter(id -> !deletedIds.contains(id)).collect(Collectors.toSet());

		courseRepository.deleteAll(existing);
		log.info("Deleted {} course(s); not found: {}", deletedIds.size(), notFound);
		return new DeleteResult(deletedIds, notFound);
	}

	private void attachTeacherRefs(Collection<Course> courses) {
		courses.forEach(c -> c.setTeacher(teacherRepository.getReferenceById(c.getTeacher().getId())));
	}

	private void assertTeachersExist(Collection<Course> courses) {
		var teacherIds = courses.stream().map(c -> c.getTeacher().getId()).collect(Collectors.toSet());

		var existing = new HashSet<>(teacherRepository.findExistingIds(teacherIds));
		var missing = teacherIds.stream().filter(id -> !existing.contains(id)).collect(Collectors.toSet());

		if (!missing.isEmpty()) {
			log.error("createAll: teachers not found: {}", missing);
			throw new EntityNotFoundException("Teachers not found: " + missing);
		}
	}
	
	private void assertTeacherExists(Long teacherId) {
	    if (!teacherRepository.existsById(teacherId)) {
	        log.error("updateSelf: teacher not found: id={}", teacherId);
	        throw new EntityNotFoundException("Teacher not found: id=" + teacherId);
	    }
	}

	private void assertNamesFreeInDb(Collection<String> namesLower) {
		var nameConflicts = courseRepository.findExistingNamesIgnoreCase(new HashSet<>(namesLower));
		if (!nameConflicts.isEmpty()) {
			log.warn("course names already exist in DB: {}", nameConflicts);
			throw new IllegalArgumentException("Course names already exist: " + nameConflicts);
		}
	}

	private void assertCodesFreeInDbForCreate(Collection<String> codesLower) {
		var conflicts = courseRepository.findExistingCodesIgnoreCase(new HashSet<>(codesLower));
		if (!conflicts.isEmpty()) {
			log.warn("course codes already exist in DB: {}", conflicts);
			throw new IllegalArgumentException("Course codes already exist: " + conflicts);
		}
	}
	
	private void assertCodeFreeInDbForSelfUpdate(Long courseId, String codeLower) {
	    var conflicts = courseRepository.findConflictingCodesIgnoreCase(Set.of(codeLower), List.of(courseId));
	    if (!conflicts.isEmpty()) {
	        log.warn("target course code already taken by other course(s): {}", conflicts);
	        throw new IllegalArgumentException("Course codes already exist: " + conflicts);
	    }
	}
	
	private <T> void requireValidation(T patch) {
		Optional.ofNullable(patch).orElseThrow(() -> new IllegalArgumentException("patch must not be null"));
		validator.validate(patch);
	}
	
	private Course getManagedCourse(Long id, String opName) {
	    return courseRepository.findById(id).orElseThrow(() -> {
	        log.error("{}: Course not found: id={}", opName, id);
	        return new EntityNotFoundException("Course not found: id=" + id);
	    });
	}

}
