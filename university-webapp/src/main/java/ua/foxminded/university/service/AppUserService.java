package ua.foxminded.university.service;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
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
import ua.foxminded.university.model.domain.validation.EntityValidatior;
import ua.foxminded.university.model.repository.AppUserRepository;
import ua.foxminded.university.security.PasswordPolicy;
import ua.foxminded.university.service.dto.DeleteResult;

@Service
@RequiredArgsConstructor
@Transactional
public class AppUserService {

	private static final Logger log = LoggerFactory.getLogger(AppUserService.class);

	private final AppUserRepository usersRepository;
	private final PasswordPolicy passwordPolicy;
	private final EntityValidatior validator;

	@Transactional(value = TxType.REQUIRES_NEW)
	public List<AppUser> createAdmins(Collection<AppUser> drafts) {
		if (drafts == null || drafts.isEmpty()) {
			log.warn("createAdmins: null/empty input");
			return List.of();
		}

		var prepared = drafts.stream().filter(Objects::nonNull).map(this::normalizeUserDraft).peek(u -> {
			if (u.getEmail() == null || u.getPassword() == null) {
				throw new IllegalArgumentException("email/password must not be null");
			}
		}).toList();

		if (prepared.isEmpty()) {
			log.warn("createAdmins: only nulls in input -> nothing to do");
			return List.of();
		}

		assertNoDuplicateEmailsInRequest(prepared);

		prepared.stream().map(AppUser::getEmail).forEach(email -> assertEmailUniqueForAnother(email, null));

		var admins = prepared.stream().peek(u -> validator.validateRawPassword(u.getPassword())).map(u -> {
			u.setPassword(passwordPolicy.encodeForChange(u.getPassword()));
			u.setRole(UserRole.ADMIN);
			u.setEnabled(true);
			return u;
		}).toList();

		validator.validateAll(admins);
		return usersRepository.saveAll(admins);
	}

	@Transactional(value = TxType.SUPPORTS)
	public List<AppUser> findByIds(Collection<Long> ids) {
		if (ids == null || ids.isEmpty()) {
			log.warn("findByIds called with null/empty list");
			return List.of();
		}
		var filtered = ids.stream().filter(Objects::nonNull).distinct().toList();
		return filtered.isEmpty() ? List.of() : usersRepository.findAllById(filtered);
	}

	@Transactional(value = TxType.REQUIRES_NEW)
	public void changePasswordSelf(Long userId, String currentRaw, String newRaw) {
		if (userId == null) {
			log.warn("changePasswordSelf: userId is null");
			throw new IllegalArgumentException("userId must not be null");
		}
		if (currentRaw == null || newRaw == null) {
			log.warn("changePasswordSelf: passwords are null (userId={})", userId);
			throw new IllegalArgumentException("passwords must not be null");
		}

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
		if (userId == null || patch == null) {
			log.error("updateProfileFields: invalid args: userId={}, patchIsNull={}", userId, patch == null);
			throw new IllegalArgumentException("userId/patch must not be null");
		}

		var user = usersRepository.findById(userId).orElseThrow(() -> {
			log.error("updateProfileFields: AppUser not found: id={}", userId);
			return new EntityNotFoundException("AppUser not found: id=" + userId);
		});

		var newEmail = patch.getEmail() == null ? null : patch.getEmail().trim();
		var newFirst = patch.getFirstName() == null ? null : patch.getFirstName().trim();
		var newLast = patch.getLastName() == null ? null : patch.getLastName().trim();

		if (newEmail != null && !newEmail.equalsIgnoreCase(user.getEmail())) {
			assertEmailUniqueForAnother(newEmail, user.getId());
			user.setEmail(newEmail);
		}
		if (newFirst != null)
			user.setFirstName(newFirst);
		if (newLast != null)
			user.setLastName(newLast);

		validator.validate(user);
		log.debug("updateProfileFields: updated profile fields for userId={}", userId);
	}

	@Transactional(value = TxType.REQUIRES_NEW)
	public int enableUsersByIds(Collection<Long> ids) {
		return setEnabledFlag(ids, true);
	}

	@Transactional(value = TxType.REQUIRES_NEW)
	public int disableUsersByIds(Collection<Long> ids) {
		return setEnabledFlag(ids, false);
	}

	@Transactional(value = TxType.REQUIRES_NEW)
	public DeleteResult deleteAdminsByIds(Collection<Long> ids) {
		if (ids == null || ids.isEmpty()) {
			log.warn("deleteAdminsByIds: null/empty ids");
			return new DeleteResult(List.of(), List.of());
		}

		var distinct = ids.stream().filter(Objects::nonNull).distinct().toList();
		if (distinct.isEmpty()) {
			log.warn("deleteAdminsByIds: only nulls in ids -> nothing to do");
			return new DeleteResult(List.of(), List.of());
		}

		var found = usersRepository.findAllById(distinct);
		var foundIds = found.stream().map(AppUser::getId).collect(Collectors.toSet());
		var notFound = distinct.stream().filter(id -> !foundIds.contains(id)).toList();

		if (found.isEmpty()) {
			log.info("deleteAdminsByIds: nothing exists among {}", distinct);
			return new DeleteResult(List.of(), notFound);
		}

		var notAdmins = found.stream().filter(u -> u.getRole() != UserRole.ADMIN).map(AppUser::getId).toList();

		if (!notAdmins.isEmpty()) {
			log.error("deleteAdminsByIds: not admin ids: {}", notAdmins);
			throw new IllegalStateException("Only ADMIN users can be deleted by this operation: " + notAdmins);
		}

		long totalAdmins = usersRepository.countByRole(UserRole.ADMIN);
		if (totalAdmins - found.size() <= 0) {
			log.error("deleteAdminsByIds: attempt to delete the last admin(s)");
			throw new IllegalStateException("Cannot delete the last admin user");
		}

		usersRepository.deleteAll(found);
		return new DeleteResult(foundIds.stream().toList(), notFound);
	}

	private int setEnabledFlag(Collection<Long> ids, boolean enabled) {
		if (ids == null || ids.isEmpty()) {
			log.warn("setEnabledFlag: null/empty ids (enabled={})", enabled);
			return 0;
		}
		var distinct = ids.stream().filter(Objects::nonNull).distinct().toList();
		if (distinct.isEmpty()) {
			log.warn("setEnabledFlag: only nulls in ids -> nothing to do (enabled={})", enabled);
			return 0;
		}

		var existing = assertAllIdsExist(distinct);
		if (!enabled) {
			assertNotDisablingLastEnabledAdmin(existing);
		}

		int updated = usersRepository.setEnabledForIds(existing, enabled);
		log.info("setEnabledFlag: updated enabled={} for {} user(s)", enabled, updated);
		return updated;
	}

	private Set<Long> assertAllIdsExist(Collection<Long> ids) {
		var existing = new java.util.HashSet<>(usersRepository.findExistingIds(ids));
		var missing = ids.stream().filter(id -> !existing.contains(id)).toList();
		if (!missing.isEmpty()) {
			log.error("setEnabledFlag: some ids do not exist: {}", missing);
			throw new EntityNotFoundException("Users not found: " + missing);
		}
		return existing;
	}

	private void assertNotDisablingLastEnabledAdmin(Collection<Long> targetExistingIds) {
		long enabledAdminsTotal = usersRepository.countByRoleAndEnabled(UserRole.ADMIN, true);
		if (enabledAdminsTotal <= 0)
			return;

		int targetedEnabledAdmins = usersRepository.findEnabledIdsByRoleIn(targetExistingIds, UserRole.ADMIN).size();

		long remaining = enabledAdminsTotal - targetedEnabledAdmins;
		if (remaining <= 0) {
			log.error("setEnabledFlag: attempt to disable the last enabled admin(s)");
			throw new IllegalStateException("Cannot disable the last enabled admin user");
		}
	}

	private void assertEmailUniqueForAnother(String email, Long selfId) {
		if (email == null)
			return;

		boolean taken = (selfId == null) ? usersRepository.existsByEmailIgnoreCase(email)
				: usersRepository.existsByEmailIgnoreCaseAndIdNot(email, selfId);

		if (taken) {
			log.warn("assertEmailUniqueForAnother: email already taken: email='{}', requesterUserId={}", email, selfId);
			throw new IllegalArgumentException("Email is already taken");
		}
		log.debug("assertEmailUniqueForAnother: email '{}' is available for userId={}", email, selfId);
	}

	private AppUser normalizeUserDraft(AppUser u) {
		u.setId(null);
		if (u.getEmail() != null)
			u.setEmail(u.getEmail().trim());
		if (u.getFirstName() != null)
			u.setFirstName(u.getFirstName().trim());
		if (u.getLastName() != null)
			u.setLastName(u.getLastName().trim());
		return u;
	}

	private void assertNoDuplicateEmailsInRequest(Collection<AppUser> users) {
		var seen = new HashSet<String>();
		var dups = new java.util.ArrayList<String>();
		for (var u : users) {
			var email = u.getEmail();
			if (email == null)
				continue;
			var key = email.toLowerCase();
			if (!seen.add(key))
				dups.add(email);
		}
		if (!dups.isEmpty()) {
			log.error("createAdmins: duplicate emails in request: {}", dups);
			throw new IllegalArgumentException("Duplicate emails in request: " + dups);
		}
	}
}
