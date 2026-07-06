package ua.foxminded.university.model.domain;

import java.time.OffsetDateTime;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder(toBuilder = true)
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "student")
@SQLDelete(sql = "update student set deleted_at = now() where id = ? and deleted_at is null")
@SQLRestriction("deleted_at is null")
public class Student {

    @Id
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    @MapsId
    @OneToOne(optional = false, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "id")
    private AppUser user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private StudyGroup group;

    @ToString.Include
    @Column()
    private Integer enrollmentYear;
    
    @Column()
    private OffsetDateTime deletedAt;
}
