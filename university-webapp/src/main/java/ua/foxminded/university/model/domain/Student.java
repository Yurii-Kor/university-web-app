package ua.foxminded.university.model.domain;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;
import ua.foxminded.university.model.domain.enums.UserRole;
import ua.foxminded.university.model.domain.validation.OnCreate;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder(toBuilder = true)
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "student")
public class Student {

    @Id
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    @NotNull @Valid
    @MapsId
    @OneToOne(optional = false, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "id")
    private AppUser user;

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private StudyGroup group;

    @NotNull @Min(2000) @Max(2100)
    @ToString.Include
    @Column(name = "enrollment_year", nullable = false)
    private Integer enrollmentYear;

    @AssertTrue(message = "Student.user.role must be STUDENT")
    private boolean isUserRoleStudent() {
        return user != null && user.getRole() == UserRole.STUDENT;
    }
    
    @AssertTrue(message = "Student.group.id must not be null", groups = OnCreate.class)
    private boolean hasGroupIdOnCreate() {
        return group != null && group.getId() != null;
    }
}

