package ua.foxminded.university.model.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "groups")
@SQLDelete(sql = "update groups set deleted_at = now() where id = ? and deleted_at is null")
@SQLRestriction("deleted_at is null")
public class StudyGroup {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@EqualsAndHashCode.Include
	@ToString.Include
	private Long id;

	@Column()
	@ToString.Include
	private String name;

	@Builder.Default
	@OneToMany(mappedBy = "group")
	private List<Student> students = new ArrayList<>();

	@Builder.Default
	@ManyToMany(mappedBy = "groups")
	private Set<Course> courses = new LinkedHashSet<>();

	@Column()
    private OffsetDateTime deletedAt;
}
