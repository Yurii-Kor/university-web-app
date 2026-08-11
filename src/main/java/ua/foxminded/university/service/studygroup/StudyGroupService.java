package ua.foxminded.university.service.studygroup;

import java.util.*;
import java.util.stream.Collectors;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import ua.foxminded.university.model.domain.StudyGroup;
import ua.foxminded.university.model.persistence.studygroup.StudyGroupRepository;
import ua.foxminded.university.model.persistence.studygroup.projection.GroupCardView;
import ua.foxminded.university.model.persistence.studygroup.projection.GroupView;
import ua.foxminded.university.model.persistence.studygroup.projection.StudentGroupHeaderView;
import ua.foxminded.university.service.studygroup.dto.StudyGroupCreateDto;
import ua.foxminded.university.service.studygroup.dto.StudyGroupRenameDto;
import ua.foxminded.university.service.studygroup.exception.StudyGroupDeleteException;
import ua.foxminded.university.service.studygroup.exception.StudyGroupRenameException;
import ua.foxminded.university.service.util.validation.EntityValidatior;
import ua.foxminded.university.service.util.DtoMapper;

@Service
@RequiredArgsConstructor
@Transactional
public class StudyGroupService {

	private static final Logger log = LoggerFactory.getLogger(StudyGroupService.class);

	private final StudyGroupRepository groupRepository;
	private final EntityValidatior validator;
	private final DtoMapper mapper;
	
	
    @Transactional(value = TxType.REQUIRES_NEW)
    public StudyGroup create(StudyGroupCreateDto draft) {
        validator.validate(draft);

        var group = mapper.toStudyGroupEntity(draft);

        if (groupRepository.existsByNameIgnoreCase(group.getName())) {
            throw new IllegalArgumentException("Study group name already exists: " + group.getName());
        }

        return groupRepository.save(group);
    }

	@Transactional(value = TxType.SUPPORTS)
	public List<StudyGroup> findByIds(Collection<Long> ids) {
		var distinct = Optional.ofNullable(ids)
				.orElseGet(Collections::emptyList)
				.stream()
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());

		if (distinct.isEmpty()) {
			log.warn("findByIds: null/empty input or only nulls after filtering");
			return List.of();
		}
		return groupRepository.findAllById(distinct);
	}
	
	@Transactional(value = TxType.SUPPORTS)
	public Page<GroupCardView> listGroupCardsForAdmin(Pageable pageable) {
	    return groupRepository.findGroupCardsAll(pageable);
	}
	
    @Transactional(value = TxType.SUPPORTS)
    public Page<GroupCardView> listGroupCardsForTeacher(long teacherId, Pageable pageable) {
        return groupRepository.findGroupCardsByTeacherId(teacherId, pageable);
    }
    
    @Transactional(value = TxType.SUPPORTS)
    public StudentGroupHeaderView getGroupHeaderForStudent(long studentId) {
        return groupRepository.findStudentGroupHeader(studentId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "StudyGroup not found for student: id=" + studentId));
    }
	
	@Transactional(value = TxType.SUPPORTS)
	public List<GroupView> listGroupOptions() {
	    return groupRepository.findGroupOptions();
	}

    @Transactional(value = TxType.REQUIRES_NEW)
    public StudyGroup rename(StudyGroupRenameDto patch) {
        Optional.ofNullable(patch).orElseThrow(() -> new IllegalArgumentException("patch must not be null"));
        validator.validateWithId(patch, patch.id());

        var managed = groupRepository.findById(patch.id()).orElseThrow(() -> {
            return new EntityNotFoundException("StudyGroup not found: id=" + patch.id());
        });

        applyNamePatch(managed, patch);

        return managed;
    }

    @Transactional(value = TxType.REQUIRES_NEW)
    public void delete(long id) {
        if (!groupRepository.existsById(id)) {
            throw new EntityNotFoundException("StudyGroup not found: id=" + id);
        }

        var studentCount = groupRepository.countStudentsByGroupId(id);

        if (studentCount > 0) {
            throw new StudyGroupDeleteException(id, studentCount,
                    "Cannot delete non-empty group: id=%d, students=%d".formatted(id, studentCount));
        }

        groupRepository.deleteById(id);

        log.info("delete: deleted StudyGroup: id={}", id);
    }

    private void applyNamePatch(StudyGroup managed, StudyGroupRenameDto patch) {

        Optional.ofNullable(patch.name())
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .filter(name -> isDifferentIgnoreCase(name, managed.getName()))
                .ifPresent(name -> {
                    if (groupRepository.existsByNameIgnoreCase(name)) {

                        throw new StudyGroupRenameException(managed.getId(), name,
                                "Study group id=%d cannot be renamed: name '%s' already exists"
                                        .formatted(managed.getId(), name));
                    }

                    managed.setName(name);

                    log.debug("rename: name updated " + "(groupId={})", managed.getId());
                });
    }

    private boolean isDifferentIgnoreCase(String newValue, String currentValue) {
        return Optional.ofNullable(currentValue).map(current -> !current.equalsIgnoreCase(newValue)).orElse(true);
    }
}
