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
import ua.foxminded.university.model.domain.validation.EntityValidatior;
import ua.foxminded.university.model.domain.validation.OnCreate;
import ua.foxminded.university.model.repository.CourseRepository;
import ua.foxminded.university.model.repository.StudyGroupRepository;
import ua.foxminded.university.model.repository.TeacherRepository;
import ua.foxminded.university.service.dto.DeleteResult;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CourseService {
	
	private final Integer NOT_UPDATED  = 0;

	private static final Logger log = LoggerFactory.getLogger(CourseService.class);

	private final CourseRepository courseRepository;
	private final TeacherRepository teacherRepository;
	private final StudyGroupRepository groupRepository;
	private final EntityValidatior validator;

	@Transactional(value = TxType.REQUIRES_NEW)
	public List<Course> createAll(Collection<Course> courses) {
		var toPersist = normalizeCoursesToPersist(courses);
		if (toPersist.isEmpty()) {
			log.warn("createAll: nothing to persist (null/empty input or all items null)");
			return List.of();
		}
	    
		validator.validateAll(toPersist, OnCreate.class);

		assertTeachersExist(toPersist);
		attachTeacherRefs(toPersist);

		assertGroupsExist(toPersist);
		attachGroupRefs(toPersist);

		var codes = toPersist.stream().map(Course::getCode).toList();
		assertNoDuplicateCodesInRequest(codes);
		assertCodesFreeInDb(codes);

		var names = toPersist.stream().map(Course::getName).map(s -> s.trim().toLowerCase()).toList();
		assertNoDuplicateNamesInRequest(names);
		assertNamesFreeInDb(names);

		return courseRepository.saveAll(toPersist);
	}

	@Transactional(value = TxType.SUPPORTS)
	public List<Course> findByIds(Collection<Long> ids) {
		var distinct = Optional.ofNullable(ids)
				.orElseGet(Collections::emptySet)
				.stream()
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());

		if (distinct.isEmpty()) {
			log.warn("findByIds: null/empty input or only nulls after filtering");
			return List.of();
		}

		return courseRepository.findAllById(distinct);
	}

	@Transactional(value = TxType.REQUIRES_NEW)
	public Course updateSelf(Course patch) {
		Optional.ofNullable(patch).map(Course::getId).orElseThrow(() -> {
			log.error("updateSelf: invalid args: patch or id is null");
			return new IllegalArgumentException("course.id must not be null for update");
		});

		var managed = courseRepository.findById(patch.getId()).orElseThrow(() -> {
			log.error("updateSelf: Course not found: id={}", patch.getId());
			return new EntityNotFoundException("Course not found: id=" + patch.getId());
		});
		
		Optional.ofNullable(patch.getName())
				.map(String::trim)
				.filter(newName -> !newName.equalsIgnoreCase(managed.getName()))
				.ifPresent(newName -> {
					assertNamesFreeInDb(List.of(newName.toLowerCase()));
					managed.setName(newName);
				});
		
		Optional.ofNullable(patch.getDescription())
				.map(String::trim)
				.ifPresent(desc -> managed.setDescription(desc.isEmpty() ? null : desc));

		validator.validateAll(List.of(managed));
		log.debug("updateSelf: updated name/description for courseId={}", managed.getId());
		return managed;
	}

	@Transactional(value = TxType.REQUIRES_NEW)
	public Integer updateCodes(Map<Long, String> codeByCourseId) {
		return Optional.ofNullable(codeByCourseId)
				.filter(Predicate.not(Map::isEmpty))
				.map(this::doUpdateCodes)
				.orElseGet(() -> {
					log.warn("updateCodes: empty/null map");
					return NOT_UPDATED;
				});
	}

	private Integer doUpdateCodes(Map<Long, String> codeByCourseId) {
	    assertIdsNotNull(codeByCourseId);
	    assertCodesNotNull(codeByCourseId);

	    var normalized = codeByCourseId.entrySet().stream()
	            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().trim().toUpperCase()));

	    var ids   = List.copyOf(normalized.keySet());
	    var codes = List.copyOf(normalized.values());

	    assertNoDuplicateCodesInRequest(codes);
	    validator.validateCourseCodes(codes);

	    assertCourseIdsExist(ids);
	    assertCodesFreeInDb(normalized);

	    var existing = courseRepository.findAllById(ids);
	    existing.forEach(c -> c.setCode(normalized.get(c.getId())));
	    log.info("updateCodes: updated {}", existing.size());
	    return existing.size();
	}

	@Transactional(value = TxType.REQUIRES_NEW)
	public Integer addGroupsToCourse(Long courseId, Collection<Long> groupIds) {
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
		Optional.of(missing).filter(Predicate.not(Collection::isEmpty)).ifPresent(m -> {
			log.error("addGroupsToCourse: StudyGroups not found: {}", m);
			throw new EntityNotFoundException("StudyGroups not found: " + m);
		});

		return attachExistingGroupsToCourse(courseId, existing);
	}

	private Integer attachExistingGroupsToCourse(Long courseId, Set<Long> existingGroupIds) {
		var course = courseRepository.findById(courseId).orElseThrow(() -> {
			log.error("addGroupsToCourse: course not found: id={}", courseId);
			return new EntityNotFoundException("Course not found: id=" + courseId);
		});

		var groupsSet = Optional.ofNullable(course.getGroups()).orElseGet(() -> {
			course.setGroups(new LinkedHashSet<StudyGroup>());
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
	public Integer removeGroupsFromCourse(Long courseId, Collection<Long> groupIds) {
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
			log.warn("addGroupsToCourse: null/empty groupIds or only nulls -> nothing to do");
			return NOT_UPDATED;
		}
		
		return doRemoveGroupsFromCourse(courseId, distinct);
	}
	
	private Integer doRemoveGroupsFromCourse(Long courseId, Collection<Long> groupIds) {
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

	private List<Course> normalizeCoursesToPersist(Collection<Course> coursesToPersist) {
		return Optional.ofNullable(coursesToPersist)
				.orElseGet(List::of)
				.stream()
				.filter(Objects::nonNull)
				.map(this::normalizeCourseInPlace)
				.toList();
	}

	private Course normalizeCourseInPlace(Course c) {
		c.setId(null);
		Optional.ofNullable(c.getCode()).map(String::trim).map(String::toUpperCase).ifPresent(c::setCode);
		Optional.ofNullable(c.getName()).map(String::trim).ifPresent(c::setName);
		
		return c;
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

	private void assertGroupsExist(Collection<Course> courses) {
		var groupIds = getGroupsIds(courses);

		if (groupIds.isEmpty())	return;

		var existing = new HashSet<>(groupRepository.findExistingIds(groupIds));
		var missing = groupIds.stream().filter(id -> !existing.contains(id)).collect(Collectors.toSet());

		if (!missing.isEmpty()) {
			log.error("createAll: study groups not found: {}", missing);
			throw new EntityNotFoundException("StudyGroups not found: " + missing);
		}
	}

	private void assertCourseIdsExist(Collection<Long> ids) {
		Optional.ofNullable(ids).filter(c -> c.stream().noneMatch(Objects::isNull)).orElseThrow(() -> {
			log.error("assertCourseIdsExist: list contains null id");
			return new IllegalArgumentException("courseId must not be null");
		});

		var distinct = ids.stream().collect(Collectors.toSet());

		var existing = new HashSet<>(courseRepository.findExistingIds(distinct));
		var missing = distinct.stream().filter(id -> !existing.contains(id)).collect(Collectors.toSet());

		if (!missing.isEmpty()) {
			log.error("assertCourseIdsExist: missing course ids {}", missing);
			throw new EntityNotFoundException("Courses not found: " + missing);
		}
	}

	private void assertNamesFreeInDb(Collection<String> names) {
		var nameConflicts = courseRepository.findExistingNamesIgnoreCase(new HashSet<>(names));
		if (!nameConflicts.isEmpty()) {
			log.warn("course names already exist in DB: {}", nameConflicts);
			throw new IllegalArgumentException("Course names already exist: " + nameConflicts);
		}
	}

	private void assertCodesFreeInDb(Collection<String> codes) {
		var normalized = codes.stream()
				.filter(Objects::nonNull)
				.map(s -> s.trim().toLowerCase())
				.collect(Collectors.toSet());

		if (normalized.isEmpty()) return;

		var conflicts = courseRepository.findExistingCodesIgnoreCase(normalized);
		if (!conflicts.isEmpty()) {
			log.warn("course codes already exist in DB: {}", conflicts);
			throw new IllegalArgumentException("Course codes already exist: " + conflicts);
		}
	}

	private void assertCodesFreeInDb(Map<Long, String> codeById) {
		var codes = codeById.values()
				.stream()
				.filter(Objects::nonNull)
				.map(s -> s.trim().toLowerCase())
				.collect(Collectors.toSet());
		
		var ids = List.copyOf(codeById.keySet());
		if (codes.isEmpty()) return;

		var conflicts = courseRepository.findConflictingCodesIgnoreCase(codes, ids);
		if (!conflicts.isEmpty()) {
			log.warn("target course codes already taken by other courses: {}", conflicts);
			throw new IllegalArgumentException("Course codes already exist: " + conflicts);
		}
	}

	private void attachTeacherRefs(Collection<Course> courses) {
		courses.forEach(c -> c.setTeacher(teacherRepository.getReferenceById(c.getTeacher().getId())));
	}

	private void attachGroupRefs(Collection<Course> courses) {
		courses.forEach(c -> {
			var groups = c.getGroups();
			if (Optional.ofNullable(groups).map(Collection::isEmpty).orElse(true)) return;

			var refs = groups.stream()
					.filter(Objects::nonNull)
					.map(StudyGroup::getId)
					.filter(Objects::nonNull)
					.map(groupRepository::getReferenceById)
					.collect(Collectors.toCollection(LinkedHashSet::new));
			
			c.setGroups(refs);
		});
	}

	private Set<Long> getGroupsIds(Collection<Course> courses) {
		return courses.stream()
				.map(Course::getGroups)
				.filter(Objects::nonNull)
				.flatMap(Set::stream)
				.filter(Objects::nonNull)
				.map(StudyGroup::getId)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());
	}

	private void assertNoDuplicateCodesInRequest(Collection<String> codes) {
		var dup = codes.stream()
				.filter(Objects::nonNull)
				.map(s -> s.trim().toLowerCase())
				.collect(Collectors.groupingBy(s -> s, Collectors.counting()))
				.entrySet()
				.stream()
				.filter(e -> e.getValue() > 1)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());

		if (!dup.isEmpty()) {
			log.warn("duplicate normalized course codes in request: {}", dup);
			throw new IllegalArgumentException("Duplicate normalized course codes in request: " + dup);
		}
	}

	private void assertNoDuplicateNamesInRequest(Collection<String> names) {
		var dup = names.stream()
				.filter(Objects::nonNull)
				.map(s -> s.trim().toLowerCase())
				.collect(Collectors.groupingBy(s -> s, Collectors.counting()))
				.entrySet()
				.stream()
				.filter(e -> e.getValue() > 1)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());

		if (!dup.isEmpty()) {
			log.warn("duplicate normalized course names in request: {}", dup);
			throw new IllegalArgumentException("Duplicate normalized course names in request: " + dup);
		}
	}

	private void assertIdsNotNull(Map<Long, String> codeByCourseId) {
		if (codeByCourseId.keySet().stream().anyMatch(Objects::isNull)) {
			log.error("updateCodes: map contains null key");
			throw new IllegalArgumentException("courseId must not be null");
		}
	}

	private void assertCodesNotNull(Map<Long, String> codeByCourseId) {
		if (codeByCourseId.values().stream().anyMatch(Objects::isNull)) {
			log.error("updateCodes: map contains null code");
			throw new IllegalArgumentException("course code must not be null");
		}
	}
}
