package ua.foxminded.university.service;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import lombok.RequiredArgsConstructor;
import ua.foxminded.university.model.domain.AppUser;
import ua.foxminded.university.model.domain.enums.UserRole;
import ua.foxminded.university.model.repository.AppUserRepository;
import ua.foxminded.university.security.PasswordPolicy;
import ua.foxminded.university.service.dto.request.AppUserDto;
import ua.foxminded.university.service.dto.response.DeleteResult;
import ua.foxminded.university.service.util.DtoMapper;
import ua.foxminded.university.service.util.DuplicateGuard;
import ua.foxminded.university.service.util.RequestDtoNormalizer;
import ua.foxminded.university.service.util.validation.EntityValidatior;
import ua.foxminded.university.service.util.validation.groups.OnChangePassword;
import ua.foxminded.university.service.util.validation.groups.OnCreate;
import ua.foxminded.university.service.util.validation.groups.OnUpdateSelf;

@Service
@RequiredArgsConstructor
@Transactional
public class AppUserService {

	private static final int NOT_UPDATED = 0;

	private static final Logger log = LoggerFactory.getLogger(AppUserService.class);

	private final AppUserRepository usersRepository;
	private final PasswordPolicy passwordPolicy;
	private final EntityValidatior validator;

	private final RequestDtoNormalizer normalizer;
	private final DtoMapper mapper;
	private final DuplicateGuard duplicateGuard;

	@Transactional(value = TxType.REQUIRES_NEW)
	public List<AppUser> createAdmins(Collection<AppUserDto> drafts) {
		var norm = normalizer.normalizeUsers(drafts);
		if (norm.isEmpty()) {
			log.warn("createAdmins: nothing to persist (null/empty input)");
			return List.of();
		}

		validator.validateAll(norm, OnCreate.class);

		var emails = norm.stream()
				.map(AppUserDto::email)
				.filter(Objects::nonNull)
				.map(String::trim)
				.map(String::toLowerCase)
				.toList();
		duplicateGuard.assertNoDuplicates(emails, "emails");
		assertEmailsFreeInDb(emails);

		var admins = mapper.toAppUserEntities(norm);

		admins.forEach(a -> {
			a.setPassword(passwordPolicy.encodePassword(a.getPassword()));
			a.setRole(UserRole.ADMIN);
		});

		return usersRepository.saveAll(admins);
	}

	@Transactional(value = TxType.SUPPORTS)
	public List<AppUser> findByIds(Collection<Long> ids) {
		var distinct = Optional.ofNullable(ids)
				.orElseGet(Collections::emptyList)
				.stream()
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());

		if (distinct.isEmpty()) {
			log.warn("findByIds: null/empty input or only nulls after filtering");
			return List.of();
		}

		return usersRepository.findAllById(distinct);
	}

	@Transactional(value = TxType.REQUIRES_NEW)
	public void changePasswordSelf(AppUserDto dto) {
		var norm = normalizer.normalizeUser(dto)
				.orElseThrow(() -> new IllegalArgumentException("DTO must not be null"));
		validator.validate(norm, OnChangePassword.class);

		var user = usersRepository.findById(norm.id()).orElseThrow(() -> {
			log.warn("changePasswordSelf: user not found (userId={})", norm.id());
			return new EntityNotFoundException("User not found: id=" + norm.id());
		});

		passwordPolicy.assertCurrentMatches(norm.currentPassword(), user.getPassword());
		passwordPolicy.assertNewDifferentFromCurrent(norm.currentPassword(), norm.newPassword());
		
		user.setPassword(passwordPolicy.encodePassword(norm.newPassword()));
		log.info("changePasswordSelf: password changed (userId={})", norm.id());
	}

	@Transactional(TxType.REQUIRES_NEW)
	public void updateProfileFields(AppUserDto patchDto) {
		var norm = normalizer.normalizeUser(patchDto)
				.orElseThrow(() -> new IllegalArgumentException("DTO must not be null"));

		validator.validate(norm, OnUpdateSelf.class);

		var user = usersRepository.findById(norm.id()).orElseThrow(() -> {
			log.error("updateProfileFields: AppUser not found: id={}", norm.id());
			return new EntityNotFoundException("AppUser not found: id=" + norm.id());
		});

		Optional.ofNullable(norm.email()).ifPresent(newEmail -> {
			var same = Optional.ofNullable(user.getEmail()).filter(newEmail::equals).isPresent();
			if (!same) {
				assertEmailUniqueForAnother(newEmail, user.getId());
				user.setEmail(newEmail);
			}
		});

		Optional.ofNullable(norm.firstName())
				.filter(newFirst -> !newFirst.equals(Optional.ofNullable(user.getFirstName()).orElse(null)))
				.ifPresent(user::setFirstName);

		Optional.ofNullable(norm.lastName())
				.filter(newLast -> !newLast.equals(Optional.ofNullable(user.getLastName()).orElse(null)))
				.ifPresent(user::setLastName);

		log.debug("updateProfileFields: updated profile fields for userId={}", norm.id());
	}

	@Transactional(value = TxType.REQUIRES_NEW)
	public Integer enableUsersByIds(Collection<Long> ids) {
		return setEnabledFlag(ids, true);
	}

	@Transactional(value = TxType.REQUIRES_NEW)
	public Integer disableUsersByIds(Collection<Long> ids) {
		return setEnabledFlag(ids, false);
	}

	@Transactional(value = TxType.REQUIRES_NEW)
	public DeleteResult deleteAdminsByIds(Collection<Long> ids) {
		var distinct = Optional.ofNullable(ids)
				.orElseGet(List::of)
				.stream()
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());

		if (distinct.isEmpty()) {
			log.warn("deleteAdminsByIds: null/empty ids or only nulls -> nothing to do");
			return new DeleteResult(Set.of(), Set.of());
		}

		var found = usersRepository.findAllById(distinct);
		var foundIds = found.stream().map(AppUser::getId).collect(Collectors.toSet());
		var notFound = distinct.stream().filter(id -> !foundIds.contains(id)).collect(Collectors.toSet());

		if (found.isEmpty()) {
			log.info("deleteAdminsByIds: nothing exists among {}", distinct);
			return new DeleteResult(Set.of(), notFound);
		}

		assertAllAdmins(found);
		assertNotDeletingLastAdmin(found.size());

		usersRepository.deleteAll(found);
		return new DeleteResult(foundIds, notFound);
	}

	private void assertEmailsFreeInDb(Collection<String> emails) {
		var normalized = emails.stream()
				.filter(Objects::nonNull)
				.map(String::trim)
				.map(String::toLowerCase)
				.collect(Collectors.toSet());
		
		if (normalized.isEmpty()) return;
		
		var conflicts = usersRepository.findExistingEmailsIgnoreCase(normalized);

		if (!conflicts.isEmpty()) {
			log.warn("createAdmins: emails already exist in DB: {}", conflicts);
			throw new IllegalArgumentException("Emails already exist: " + conflicts);
		}
	}

	private Integer setEnabledFlag(Collection<Long> ids, boolean enabled) {
		var distinct = Optional.ofNullable(ids)
				.orElseGet(List::of)
				.stream()
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());

		if (distinct.isEmpty()) {
			log.warn("setEnabledFlag: only nulls in ids -> nothing to do (enabled={})", enabled);
			return NOT_UPDATED;
		}

		var existing = assertAllIdsExist(distinct);
		if (!enabled) {
			assertNotDisablingLastEnabledAdmin(existing);
		}

		var updated = usersRepository.setEnabledForIds(existing, enabled);
		log.info("setEnabledFlag: updated enabled={} for {} user(s)", enabled, updated);
		return updated;
	}

	private Set<Long> assertAllIdsExist(Collection<Long> ids) {
		var existing = new HashSet<>(usersRepository.findExistingIds(ids));
		var missing = ids.stream().filter(id -> !existing.contains(id)).collect(Collectors.toSet());
		if (!missing.isEmpty()) {
			log.error("setEnabledFlag: some ids do not exist: {}", missing);
			throw new EntityNotFoundException("Users not found: " + missing);
		}

		return existing;
	}

	private void assertNotDisablingLastEnabledAdmin(Collection<Long> targetExistingIds) {
		var enabledAdminsTotal = usersRepository.countByRoleAndEnabled(UserRole.ADMIN, true);
		if (enabledAdminsTotal <= 0)
			return;

		var targetedEnabledAdmins = usersRepository.findEnabledIdsByRoleIn(targetExistingIds, UserRole.ADMIN).size();

		var remaining = enabledAdminsTotal - targetedEnabledAdmins;
		if (remaining <= 0) {
			log.error("setEnabledFlag: attempt to disable the last enabled admin(s)");
			throw new IllegalStateException("Cannot disable the last enabled admin user");
		}
	}

	private void assertEmailUniqueForAnother(String email, Long selfId) {
		if (usersRepository.existsByEmailIgnoreCaseAndIdNot(email, selfId)) {
			log.warn("assertEmailUniqueForAnother: email already taken: email='{}', requesterUserId={}", email, selfId);
			throw new IllegalArgumentException("Email is already taken");
		}
		log.debug("assertEmailUniqueForAnother: email '{}' is available for userId={}", email, selfId);
	}

	private void assertAllAdmins(Collection<AppUser> users) {
		var notAdmins = users.stream().filter(u -> u.getRole() != UserRole.ADMIN).map(AppUser::getId).toList();

		Optional.of(notAdmins).filter(list -> !list.isEmpty()).ifPresent(ids -> {
			log.error("deleteAdminsByIds: not admin ids: {}", ids);
			throw new IllegalStateException("Only ADMIN users can be deleted by this operation: " + ids);
		});
	}

	private void assertNotDeletingLastAdmin(int toDeleteCount) {
		var totalAdmins = usersRepository.countByRole(UserRole.ADMIN);

		Optional.of(totalAdmins - toDeleteCount).filter(remaining -> remaining > 0).orElseThrow(() -> {
			log.error("deleteAdminsByIds: attempt to delete the last admin(s)");
			return new IllegalStateException("Cannot delete the last admin user");
		});
	}
}
