package ua.foxminded.university.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
import ua.foxminded.university.service.dto.response.CourseGroupsView;
import ua.foxminded.university.service.exception.course.CourseCreateException;
import ua.foxminded.university.service.exception.course.CourseSelfUpdateException;
import ua.foxminded.university.service.util.validation.EntityValidatior;
import ua.foxminded.university.service.util.DtoMapper;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CourseService {
	
	private static final Logger log = LoggerFactory.getLogger(CourseService.class);

	private final CourseRepository courseRepository;
	private final TeacherRepository teacherRepository;
	private final StudyGroupRepository groupRepository;
	private final EntityValidatior validator;
	private final DtoMapper mapper;
	
	@Transactional(value = TxType.REQUIRES_NEW)
	public Course create(CourseCreateDto draft) {
	    Optional.ofNullable(draft).ifPresentOrElse(p -> validator.validate(p), () -> {
	        log.warn("create: draft is null");
	        throw new IllegalArgumentException("draft must not be null");
	    });

	    if (courseRepository.existsByCodeIgnoreCase(draft.code())) {
	        log.warn("create: code conflict (requestedCode='{}')", draft.code());
	        throw new CourseCreateException(draft, "Course code already exists: " + draft.code());
	    }

	    if (courseRepository.existsByNameIgnoreCase(draft.name())) {
	        log.warn("create: name conflict (requestedName='{}')", draft.name());
	        throw new CourseCreateException(draft, "Course name already exists: " + draft.name());
	    }

	    var teacher = teacherRepository.findById(draft.teacherId()).orElseThrow(() -> {
	        log.warn("create: teacher not found (teacherId={})", draft.teacherId());
	        return new CourseCreateException(draft, "Teacher not found: id=" + draft.teacherId());
	    });

	    var course = mapper.toCourseEntity(draft).orElseThrow(() -> {
	        log.warn("create: mapper produced null Course (code='{}', name='{}')", draft.code(), draft.name());
	        return new IllegalArgumentException("Mapper produced null Course");
	    });

	    course.setTeacher(teacher);

	    return courseRepository.save(course);
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
	public Page<CourseCardView> listCourseCardsForAdmin(Pageable pageable) {
	    return courseRepository.findCourseCardsAll(pageable);
	}

	@Transactional(value = TxType.SUPPORTS)
	public Page<CourseCardView> listCourseCardsForTeacher(long teacherId, Pageable pageable) {
	    return courseRepository.findCourseCardsByTeacherId(teacherId, pageable);
	}

	@Transactional(value = TxType.SUPPORTS)
	public Page<CourseCardView> listCourseCardsForStudent(long studentId, Pageable pageable) {
	    return courseRepository.findCourseCardsByStudentId(studentId, pageable);
	}
	
	@Transactional(value = TxType.SUPPORTS)
	public CourseGroupsView getCourseGroupsView(long courseId) {
		var header = courseRepository.findCourseHeaderById(courseId).orElseThrow(() -> {
			log.warn("getCourseGroupsPage: course not found (courseId={})", courseId);
			return new EntityNotFoundException("Course not found: id=" + courseId);
		});

	    List<GroupOptionView> assigned = groupRepository.findAssignedGroupOptions(courseId);
	    List<GroupOptionView> available = groupRepository.findAvailableGroupOptions(courseId);

	    return new CourseGroupsView(header, assigned, available);
	}

	@Transactional(value = TxType.REQUIRES_NEW)
	public Course updateSelf(CourseSelfUpdateDto patch) {
		Optional.ofNullable(patch).ifPresentOrElse(p -> validator.validateWithId(p, p.id()), () -> {
			log.warn("updateSelf: patch is null");
			throw new IllegalArgumentException("patch must not be null");
		});
		
		var managed = courseRepository.findById(patch.id()).orElseThrow(() -> {
			log.warn("updateSelf: course not found (courseId={})", patch.id());
			return new EntityNotFoundException("Course not found: id=" + patch.id());
		});

	    applyCodePatch(managed, patch);
	    applyNamePatch(managed, patch);
	    applyTeacherPatch(managed, patch);

	    log.debug("updateSelf: updated code/name/teacher for courseId={}", managed.getId());
	    return managed;
	}
	
	@Transactional(value = TxType.REQUIRES_NEW)
	public Course updateDescription(CourseDescriptionUpdateDto patch) {
		Optional.ofNullable(patch).ifPresentOrElse(p -> validator.validateWithId(p, p.id()), () -> {
			log.warn("updateDescription: patch is null");
			throw new IllegalArgumentException("patch must not be null");
		});
		
		var managed = courseRepository.findById(patch.id()).orElseThrow(() -> {
			log.warn("updateDescription: course not found (courseId={})", patch.id());
			return new EntityNotFoundException("Course not found: id=" + patch.id());
		});
		
		Optional.ofNullable(patch.description())
				.map(String::trim)
				.ifPresent(desc -> managed.setDescription(desc.isEmpty() ? null : desc));

		log.debug("updateDescription: updated description for courseId={}", managed.getId());
		return managed;
	}
	
	@Transactional(TxType.REQUIRES_NEW)
	public Optional<Long> addGroupToCourse(long courseId, long groupId) {
		var course = courseRepository.findById(courseId).orElseThrow(() -> {
			log.warn("addGroupToCourse: course not found (courseId={})", courseId);
			return new EntityNotFoundException("Course not found: id=" + courseId);
		});
		
		var groups = Optional.ofNullable(course.getGroups()).orElseGet(() -> {
	        course.setGroups(new LinkedHashSet<>());
	        return course.getGroups();
	    });

	    var already = groups.stream().map(StudyGroup::getId).filter(Objects::nonNull).collect(Collectors.toSet());
	    if (already.contains(groupId)) {
	    	log.debug("addGroupToCourse: already attached (courseId={}, groupId={})", courseId, groupId);
	        return Optional.empty();
	    }
		
	    groups.add(groupRepository.getReferenceById(groupId));
	    log.debug("addGroupToCourse: attached group (courseId={}, groupId={})", courseId, groupId);
	    
		return Optional.of(groupId);
	}
	
	@Transactional(TxType.REQUIRES_NEW)
	public Optional<Long> removeGroupFromCourse(long courseId, long groupId) {
		var course = courseRepository.findById(courseId).orElseThrow(() -> {
			log.warn("removeGroupFromCourse: course not found (courseId={})", courseId);
			return new EntityNotFoundException("Course not found: id=" + courseId);
		});
		
		var groups = Optional.ofNullable(course.getGroups()).orElseGet(Collections::emptySet);
		var already = groups.stream().map(StudyGroup::getId).filter(Objects::nonNull).collect(Collectors.toSet());
		if (!already.contains(groupId)) {
			log.debug("removeGroupFromCourse: not attached (courseId={}, groupId={})", courseId, groupId);
			return Optional.empty();
		}
		
		groups.removeIf(g -> Objects.equals(g.getId(), groupId));
		log.debug("removeGroupFromCourse: detached group (courseId={}, groupId={})", courseId, groupId);
		
		return Optional.of(groupId);
	}
	
	@Transactional(value = TxType.REQUIRES_NEW)
	public void delete(long id) {
	    if (!courseRepository.existsById(id)) {
	    	log.warn("delete: course not found (courseId={})", id);
	        throw new EntityNotFoundException("Course not found: id=" + id);
	    }
	    courseRepository.deleteById(id);
	}
	
	private void applyCodePatch(Course managed, CourseSelfUpdateDto patch) {
	    Optional.ofNullable(patch.code())
	            .map(String::trim)
	            .filter(code -> !code.isEmpty())
	            .filter(code -> isDifferentIgnoreCase(code, managed.getCode()))
	            .ifPresent(code -> {
	                if (courseRepository.existsByCodeIgnoreCase(code)) {
	                    log.warn("updateSelf: code conflict (courseId={}, requestedCode='{}')", managed.getId(), code);
	                    throw new CourseSelfUpdateException(patch, "Course code already exists: " + code);
	                }

	                managed.setCode(code);
	                log.debug("updateSelf: code updated (courseId={})", managed.getId());
	            });
	}

	private void applyNamePatch(Course managed, CourseSelfUpdateDto patch) {
	    Optional.ofNullable(patch.name())
	            .map(String::trim)
	            .filter(name -> !name.isEmpty())
	            .filter(name -> isDifferentIgnoreCase(name, managed.getName()))
	            .ifPresent(name -> {
	                if (courseRepository.existsByNameIgnoreCase(name)) {
	                    log.warn("updateSelf: name conflict (courseId={}, requestedName='{}')", managed.getId(), name);
	                    throw new CourseSelfUpdateException(patch, "Course name already exists: " + name);
	                }

	                managed.setName(name);
	                log.debug("updateSelf: name updated (courseId={})", managed.getId());
	            });
	}

	private void applyTeacherPatch(Course managed, CourseSelfUpdateDto patch) {
	    Optional.ofNullable(patch.teacherId())
	            .filter(newTeacherId -> isDifferentTeacher(managed, newTeacherId))
				.ifPresent(newTeacherId -> {
					var teacher = teacherRepository.findById(newTeacherId).orElseThrow(() -> {
						log.warn("updateSelf: teacher not found (teacherId={})", newTeacherId);
						return new CourseSelfUpdateException(patch, "Teacher not found: id=" + newTeacherId);
					});

					managed.setTeacher(teacher);
					log.debug("updateSelf: teacher updated (courseId={}, teacherId={})", managed.getId(), newTeacherId);
				});
	}

	private boolean isDifferentIgnoreCase(String newValue, String currentValue) {
	    return Optional.ofNullable(currentValue)
	            .map(current -> !current.equalsIgnoreCase(newValue))
	            .orElse(true);
	}

	private boolean isDifferentTeacher(Course managed, Long newTeacherId) {
	    return Optional.ofNullable(managed.getTeacher())
	            .map(teacher -> !newTeacherId.equals(teacher.getId()))
	            .orElse(true);
	}
}
