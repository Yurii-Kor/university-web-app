package ua.foxminded.university.model.domain;

import jakarta.persistence.*;
import lombok.*;
import ua.foxminded.university.model.domain.enums.UserRole;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "app_user")
public class AppUser {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@EqualsAndHashCode.Include
	@ToString.Include
	private Long id;

	@ToString.Include
	@Column()
	private String email;

	@Column()
	@Basic(fetch = FetchType.LAZY)
	private String password;

	@Enumerated(EnumType.STRING)
	@Column()
	private UserRole role;

	@ToString.Include
	@Column()
	private String firstName;

	@ToString.Include
	@Column()
	private String lastName;

	@Builder.Default
	@ToString.Include
	@Column()
	private boolean enabled = true;

	@Column(insertable = false)
	private OffsetDateTime createdAt;

	@ToString.Include(name = "createdAt")
	public String getCreatedAtIso() {
		return Optional.ofNullable(createdAt).map(DateTimeFormatter.ISO_OFFSET_DATE_TIME::format).orElse(null);
	}
}
