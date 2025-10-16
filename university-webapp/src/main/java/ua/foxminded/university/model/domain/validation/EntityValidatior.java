package ua.foxminded.university.model.domain.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import jakarta.validation.groups.Default;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ua.foxminded.university.model.domain.AppUser;
import ua.foxminded.university.model.domain.Course;
import ua.foxminded.university.model.domain.ScheduleEntry;
import ua.foxminded.university.model.domain.StudyGroup;
import ua.foxminded.university.model.domain.Teacher;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
public class EntityValidatior {

	private static final Logger log = LoggerFactory.getLogger(EntityValidatior.class);

	private final Validator validator;

	public <T> void validate(T target, Class<?>... groups) {
		validateAll(target == null ? null : List.of(target), groups);
	}

	public <T> void validateAll(Collection<T> targets) {
		validateAll(targets, Default.class);
	}

	public <T> void validateAll(Collection<T> targets, Class<?>... groups) {
		if (targets == null || targets.isEmpty()) {
			log.warn("validateAll: targets is null or empty");
			throw new IllegalArgumentException("Targets must not be null or empty");
		}

		log.debug("validateAll: start, items={}", targets.size());

		Set<ConstraintViolation<?>> violations = targets.stream()
				.filter(Objects::nonNull)
				.flatMap(t -> validator.validate(t, groups).stream())
				.collect(Collectors.toSet());

		if (!violations.isEmpty()) {
			log.warn("validateAll: violations found: {}", violations.size());
			throw new ConstraintViolationException(violations);
		}

		log.debug("validateAll: OK (no violations)");
	}

	public void validateRawPassword(String raw) {
		var violations = validator.validateValue(AppUser.class, "password", raw, RawPassword.class);
		if (!violations.isEmpty()) {
			log.warn("validateRawPassword: violations={}", violations.size());
			throw new ConstraintViolationException(violations);
		}
	}

	public void validateTeachersOffices(List<String> values) {
		if (values == null || values.isEmpty())	return;

		Set<ConstraintViolation<?>> all = values.stream()
				.flatMap(v -> validator.validateValue(Teacher.class, "office", v).stream())
				.collect(Collectors.toSet());

		if (!all.isEmpty()) {
			log.warn("validateFieldValues: violations for Teachers offices = {}", all.size());
			throw new ConstraintViolationException(all);
		}
	}

	public void validateGroupNames(List<String> values) {
		if (values == null || values.isEmpty())	return;

		var all = values.stream()
				.flatMap(v -> validator.validateValue(StudyGroup.class, "name", v).stream())
				.collect(Collectors.toSet());

		if (!all.isEmpty()) {
			log.warn("validateGroupNames: violations count={}", all.size());
			throw new ConstraintViolationException(all);
		}
	}

	public void validateCourseCodes(List<String> codes) {
		if (codes == null || codes.isEmpty()) return;

		var all = codes.stream()
				.flatMap(v -> validator.validateValue(Course.class, "code", v).stream())
				.collect(Collectors.toSet());

		if (!all.isEmpty()) {
			log.warn("validateCourseCodes: violations = {}", all.size());
			throw new ConstraintViolationException(all);
		}
	}

	public void validateScheduleEntryRoom(String roomNumber) {
		var violations = validator.validateValue(ScheduleEntry.class, "room", roomNumber);
		if (!violations.isEmpty()) {
			log.warn("validateScheduleEntryRoom: violations={}", violations.size());
			throw new ConstraintViolationException(violations);
		}
	}
}
