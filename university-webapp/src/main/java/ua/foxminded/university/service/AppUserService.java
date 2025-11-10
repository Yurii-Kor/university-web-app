package ua.foxminded.university.service;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
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
import ua.foxminded.university.model.domain.validation.EntityValidatior;
import ua.foxminded.university.model.repository.AppUserRepository;
import ua.foxminded.university.security.PasswordPolicy;
import ua.foxminded.university.service.dto.DeleteResult;

@Service
@RequiredArgsConstructor
@Transactional
public class AppUserService {
	
	private final Integer NOT_UPDATED  = 0;

	private static final Logger log = LoggerFactory.getLogger(AppUserService.class);

	private final AppUserRepository usersRepository;
	private final PasswordPolicy passwordPolicy;
	private final EntityValidatior validator;

	@Transactional(value = TxType.REQUIRES_NEW)
	public List<AppUser> createAdmins(Collection<AppUser> drafts) {
		var prepared = normalizeAdminsToPersist(drafts);

		if (prepared.isEmpty()) {
			log.warn("createAdmins: nothing to persist (null/empty input or all items null)");
			return List.of();
		}

		assertEmailsAndPasswordsNotNull(prepared);
		assertNoDuplicateEmailsInRequest(prepared);

		var emails = prepared.stream().map(AppUser::getEmail).toList();
		assertEmailsFreeInDb(emails);

		var admins = prepareAdminsForPersist(prepared);

		validator.validateAll(admins);
		return usersRepository.saveAll(admins);
	}


	@Transactional(value = TxType.SUPPORTS)
	public List<AppUser> findByIds(Collection<Long> ids) {
		var distinct = Optional.ofNullable(ids)
				.orElseGet(Collections::emptySet)
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
	public void changePasswordSelf(Long userId, String currentRaw, String newRaw) {
		assertChangePasswordSelfArgsNotNull(userId, currentRaw, newRaw);

		validator.validateRawPassword(newRaw);

		var user = usersRepository.findById(userId).orElseThrow(() -> {
			log.warn("changePasswordSelf: user not found (userId={})", userId);
			return new EntityNotFoundException("User not found: id=" + userId);
		});

		passwordPolicy.assertCurrentMatches(currentRaw, user.getPassword());

		var encoded = passwordPolicy.encodeForChange(newRaw);
		user.setPassword(encoded);
		log.info("changePasswordSelf: password changed (userId={})", userId);
	}

	@Transactional(TxType.REQUIRES_NEW)
	public void updateProfileFields(Long userId, AppUser patch) {
		assertUpdateProfileFieldsArgsNotNull(userId, patch);

		var user = usersRepository.findById(userId).orElseThrow(() -> {
			log.error("updateProfileFields: AppUser not found: id={}", userId);
			return new EntityNotFoundException("AppUser not found: id=" + userId);
		});

		updateEmail(user, patch);
		updateFirstName(user, patch);
		updateLastName(user, patch);

		validator.validate(user);
		log.debug("updateProfileFields: updated profile fields for userId={}", userId);
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

	
	private List<AppUser> normalizeAdminsToPersist(Collection<AppUser> drafts) {
		return Optional.ofNullable(drafts)
				.orElseGet(List::of)
				.stream()
				.filter(Objects::nonNull)
				.map(this::normalizeAdmiToPersist)
				.toList();
	}
	
	private AppUser normalizeAdmiToPersist(AppUser u) {
		u.setId(null);
		Optional.ofNullable(u.getEmail()).map(String::trim).ifPresent(u::setEmail);
		Optional.ofNullable(u.getFirstName()).map(String::trim).ifPresent(u::setFirstName);
		Optional.ofNullable(u.getLastName()).map(String::trim).ifPresent(u::setLastName);

		return u;
	}
	
	private List<AppUser> prepareAdminsForPersist(List<AppUser> prepared) {
		return prepared.stream().map(this::prepareAdminForPersist).toList();
	}

	private AppUser prepareAdminForPersist(AppUser user) {
	    validator.validateRawPassword(user.getPassword());
	    user.setPassword(passwordPolicy.encodeForChange(user.getPassword()));
	    user.setRole(UserRole.ADMIN);
	    user.setEnabled(true);
	    return user;
	}
	
	private void assertEmailsAndPasswordsNotNull(Collection<AppUser> users) {
		users.forEach(u -> Optional.ofNullable(u)
				.filter(user -> user.getEmail() != null && user.getPassword() != null)
				.orElseThrow(() -> {
					log.error("createAdmins: email/password is null for draft with id={}", u.getId());
					return new IllegalArgumentException("email/password must not be null");
				}));
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
	
	private void assertChangePasswordSelfArgsNotNull(Long userId, String currentRaw, String newRaw) {
		Optional.ofNullable(userId).orElseThrow(() -> {
			log.warn("changePasswordSelf: userId is null");
			return new IllegalArgumentException("userId must not be null");
		});

		Optional.ofNullable(currentRaw).orElseThrow(() -> {
			log.warn("changePasswordSelf: current password is null (userId={})", userId);
			return new IllegalArgumentException("passwords must not be null");
		});

		Optional.ofNullable(newRaw).orElseThrow(() -> {
			log.warn("changePasswordSelf: new password is null (userId={})", userId);
			return new IllegalArgumentException("passwords must not be null");
		});
	}
	
	private void assertUpdateProfileFieldsArgsNotNull(Long userId, AppUser patch) {
		Optional.ofNullable(userId).orElseThrow(() -> {
			log.error("updateProfileFields: invalid args: userId is null, patchIsNull={}", patch == null);
			return new IllegalArgumentException("userId/patch must not be null");
		});

		Optional.ofNullable(patch).orElseThrow(() -> {
			log.error("updateProfileFields: invalid args: userId={}, patchIsNull=true", userId);
			return new IllegalArgumentException("userId/patch must not be null");
		});
	}
	
	private void updateEmail(AppUser user, AppUser patch) {
		Optional<String> newEmailOpt = Optional.ofNullable(patch)
				.map(AppUser::getEmail)
				.map(String::trim)
				.filter(Predicate.not(String::isBlank));
		if (newEmailOpt.isEmpty()) {
			log.debug("updateProfileFields: patch.email is null/blank -> skip for userId={}", user.getId());
			return;
		}

		String newEmail = newEmailOpt.get();
		String newEmailNorm = newEmail.toLowerCase();

		var sameAsCurrent = Optional.ofNullable(user.getEmail())
				.map(String::trim)
				.map(s -> s.toLowerCase())
				.filter(Predicate.isEqual(newEmailNorm))
				.isPresent();
		if (sameAsCurrent) {
			log.debug("updateProfileFields: same email={} -> no-op for userId={}", newEmail, user.getId());
			return;
		}

		assertEmailUniqueForAnother(newEmail, user.getId());

		user.setEmail(newEmail);
	}
	
	private void updateFirstName(AppUser user, AppUser patch) {
		Optional<String> newFirstOpt = Optional.ofNullable(patch)
				.map(AppUser::getFirstName)
				.map(String::trim)
				.filter(s -> !s.isBlank());
		if (newFirstOpt.isEmpty()) {
			log.debug("updateProfileFields: patch.firstName is null/blank -> skip for userId={}", user.getId());
			return;
		}

		String newFirst = newFirstOpt.get();

		boolean sameAsCurrent = Optional.ofNullable(user.getFirstName())
				.map(String::trim)
				.filter(old -> old.equals(newFirst))
				.isPresent();
		if (sameAsCurrent) {
			log.debug("updateProfileFields: same firstName='{}' -> no-op for userId={}", newFirst, user.getId());
			return;
		}

		user.setFirstName(newFirst);
	}
	
	private void updateLastName(AppUser user, AppUser patch) {
		Optional<String> newLastOpt = Optional.ofNullable(patch)
				.map(AppUser::getLastName)
				.map(String::trim)
				.filter(s -> !s.isBlank());
		if (newLastOpt.isEmpty()) {
			log.debug("updateProfileFields: patch.lastName is null/blank -> skip for userId={}", user.getId());
			return;
		}

		String newLast = newLastOpt.get();

		var sameAsCurrent = Optional.ofNullable(user.getLastName())
				.map(String::trim)
				.filter(old -> old.equals(newLast))
				.isPresent();
		if (sameAsCurrent) {
			log.debug("updateProfileFields: same lastName='{}' -> no-op for userId={}", newLast, user.getId());
			return;
		}

		user.setLastName(newLast);
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
		if (enabledAdminsTotal <= 0) return;

		var targetedEnabledAdmins = usersRepository.findEnabledIdsByRoleIn(targetExistingIds, UserRole.ADMIN).size();

		var remaining = enabledAdminsTotal - targetedEnabledAdmins;
		if (remaining <= 0) {
			log.error("setEnabledFlag: attempt to disable the last enabled admin(s)");
			throw new IllegalStateException("Cannot disable the last enabled admin user");
		}
	}

	private void assertEmailUniqueForAnother(String email, Long selfId) {
		if (email == null) return;

		if (usersRepository.existsByEmailIgnoreCaseAndIdNot(email, selfId)) {
			log.warn("assertEmailUniqueForAnother: email already taken: email='{}', requesterUserId={}", email, selfId);
			throw new IllegalArgumentException("Email is already taken");
		}
		log.debug("assertEmailUniqueForAnother: email '{}' is available for userId={}", email, selfId);
	}

	private void assertNoDuplicateEmailsInRequest(Collection<AppUser> users) {
		var dup = users.stream()
	            .map(AppUser::getEmail)
	            .filter(Objects::nonNull)
	            .map(s -> s.trim().toLowerCase())
	            .collect(Collectors.groupingBy(s -> s, Collectors.counting()))
	            .entrySet()
	            .stream()
	            .filter(e -> e.getValue() > 1)
	            .map(Map.Entry::getKey)
	            .collect(Collectors.toSet());

	    if (!dup.isEmpty()) {
	        log.error("createAdmins: duplicate emails in request: {}", dup);
	        throw new IllegalArgumentException("Duplicate emails in request: " + dup);
	    }
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
