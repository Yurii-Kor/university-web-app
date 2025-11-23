package ua.foxminded.university.model.domain;

import java.util.Set;
import java.util.LinkedHashSet;

import jakarta.persistence.*;
import lombok.*;

import ua.foxminded.university.model.domain.enums.AcademicRank;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder(toBuilder = true)
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "teacher")
public class Teacher {

    @Id
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    @MapsId
    @OneToOne(optional = false, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "id")
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @ToString.Include
    @Column()
    private AcademicRank academicRank;

    @ToString.Include
    @Column()
    private String office;

    @OneToMany(mappedBy = "teacher", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Course> courses = new LinkedHashSet<>();
}

