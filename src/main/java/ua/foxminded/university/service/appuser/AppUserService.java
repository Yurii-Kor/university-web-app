package ua.foxminded.university.service.appuser;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import lombok.RequiredArgsConstructor;
import ua.foxminded.university.model.domain.AppUser;
import ua.foxminded.university.model.domain.enums.UserRole;
import ua.foxminded.university.model.persistence.appuser.AppUserRepository;
import ua.foxminded.university.model.persistence.appuser.projection.AdminProfileView;
import ua.foxminded.university.model.persistence.appuser.projection.AdminRowView;
import ua.foxminded.university.security.PasswordPolicy;
import ua.foxminded.university.service.appuser.dto.AppUserCreateDto;
import ua.foxminded.university.service.appuser.dto.AppUserPasswordChangeDto;
import ua.foxminded.university.service.appuser.dto.AppUserSelfUpdateDto;
import ua.foxminded.university.service.appuser.exception.AdminCreateException;
import ua.foxminded.university.service.util.DtoMapper;
import ua.foxminded.university.service.util.validation.EntityValidatior;

@Service
@RequiredArgsConstructor
@Transactional
public class AppUserService {

	private static final int ONE_ADMIN = 1;

	private static final Logger log = LoggerFactory.getLogger(AppUserService.class);

	private final AppUserRepository usersRepository;
	private final PasswordPolicy passwordPolicy;
	private final EntityValidatior validator;
	private final DtoMapper mapper;
	
	@Transactional(value = TxType.REQUIRES_NEW)
	public AppUser createAdmin(AppUserCreateDto draft) {
		validator.validate(draft);
		
		if (usersRepository.existsByEmailIgnoreCase(draft.email())) {
	        throw new AdminCreateException(draft, "Email already exists: " + draft.email());
	    }
		
		var admin = mapper.toAppUserEntity(draft);
		
		admin.setPassword(passwordPolicy.encodePassword(admin.getPassword()));
		admin.setRole(UserRole.ADMIN);

		return usersRepository.save(admin);
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
	
	@Transactional(value = TxType.SUPPORTS)
	public AdminProfileView getAdminProfileView(long id) {
		return usersRepository.findAdminProfileViewById(id)
				.orElseThrow(() -> new EntityNotFoundException("Admin user not found: id=" + id));
	}
	
	@Transactional(value = TxType.SUPPORTS)
	public Page<AdminRowView> listAdminsForView(long selfId, Pageable pageable) {
	    return usersRepository.findAdminRowsForView(selfId, pageable);
	}

	@Transactional(value = TxType.REQUIRES_NEW)
	public void changePasswordSelf(AppUserPasswordChangeDto patch) {
		validator.validate(patch);

		var managed = usersRepository.findById(patch.id())
				.orElseThrow(() -> new EntityNotFoundException("User not found: id=" + patch.id()));

		passwordPolicy.assertCurrentMatches(patch.currentPassword(), managed.getPassword());
		passwordPolicy.assertNewDifferentFromCurrent(patch.currentPassword(), patch.newPassword());
		
		managed.setPassword(passwordPolicy.encodePassword(patch.newPassword()));
		log.info("changePasswordSelf: password changed (userId={})", patch.id());
	}

	@Transactional(TxType.REQUIRES_NEW)
	public void updateProfileFields(AppUserSelfUpdateDto patch) {
		validator.validate(patch);

		var managed = usersRepository.findById(patch.id())
				.orElseThrow(() -> new EntityNotFoundException("User not found: id=" + patch.id()));

	    applyEmailPatch(managed, patch.email());
	    applyFirstNamePatch(managed, patch.firstName());
	    applyLastNamePatch(managed, patch.lastName());

	    log.debug("updateProfileFields: updated profile fields for userId={}", managed.getId());
	}
	
	@Transactional(value = TxType.REQUIRES_NEW)
	public void enableUserByIds(Long id) {
		var managed = usersRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("User not found: id=" + id));
		
		managed.setEnabled(true);
	}

	@Transactional(value = TxType.REQUIRES_NEW)
	public void disableUserByIds(long id) {
		var managed = usersRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("User not found: id=" + id));
		
		var isManagedAdmin = managed.getRole().equals(UserRole.ADMIN);
		var isLastEnabledAdmin = usersRepository.countByRoleAndEnabled(UserRole.ADMIN, true) <= ONE_ADMIN;
		if (isManagedAdmin && isLastEnabledAdmin) {
			throw new IllegalStateException("Cannot disable the last enabled admin user");
		}
		
		managed.setEnabled(false);
	}
	
	@Transactional(value = TxType.REQUIRES_NEW)
	public void deleteAdmin(long id) {
		var managed = usersRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("User not found: id=" + id));
		
		if (!UserRole.ADMIN.equals(managed.getRole())) {
	        throw new IllegalStateException("Only admin users can be deleted here");
	    }

		var isLastAdmin = usersRepository.countByRole(UserRole.ADMIN) <= ONE_ADMIN;
		if (isLastAdmin) {
			throw new IllegalStateException("Cannot delete the last admin user");
		}
		
		usersRepository.delete(managed);
	}
	
	private void applyEmailPatch(AppUser user, String newEmail) {
	    Optional.ofNullable(newEmail)
	            .filter(email -> !Objects.equals(email, user.getEmail()))
	            .ifPresent(email -> {
	            	if (usersRepository.existsByEmailIgnoreCase(email)) {
	                    throw new IllegalArgumentException("Email already exists: " + email);
	                }
	                user.setEmail(email);
	            });
	}

	private void applyFirstNamePatch(AppUser user, String newFirstName) {
	    Optional.ofNullable(newFirstName)
	            .filter(first -> !Objects.equals(first, user.getFirstName()))
	            .ifPresent(user::setFirstName);
	}

	private void applyLastNamePatch(AppUser user, String newLastName) {
	    Optional.ofNullable(newLastName)
	            .filter(last -> !Objects.equals(last, user.getLastName()))
	            .ifPresent(user::setLastName);
	}	
}
