package ua.foxminded.university.service.rolechange.target.strategy;

import java.util.Optional;

import org.springframework.stereotype.Component;

import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import lombok.RequiredArgsConstructor;
import ua.foxminded.university.model.domain.AppUser;
import ua.foxminded.university.model.domain.Teacher;
import ua.foxminded.university.model.domain.enums.UserRole;
import ua.foxminded.university.model.repository.TeacherRepository;
import ua.foxminded.university.service.rolechange.exception.RoleChangeException;
import ua.foxminded.university.service.rolechange.target.TargetRoleProfileData;
import ua.foxminded.university.service.rolechange.target.strategy.data.ToTeacherRoleProfileData;

@Component
@RequiredArgsConstructor
public class TeacherTargetRoleProfileHandler implements TargetRoleProfileHandler {

    private final TeacherRepository teacherRepository;

    @Override
    public UserRole role() {
        return UserRole.TEACHER;
    }
    
    @Override
    public Optional<Class<? extends TargetRoleProfileData>> targetDataType() {
        return Optional.of(ToTeacherRoleProfileData.class);
    }

    @Override
    @Transactional(value = TxType.MANDATORY)
    public void activateTargetProfile(AppUser user, TargetRoleProfileData data) {
        var deletedTeacher = teacherRepository.findDeletedById(user.getId());

        if (deletedTeacher.isPresent()) {
            restoreDeletedTeacher(user, deletedTeacher.get(), data);
            return;
        }

        createTeacher(user, requireTeacherData(user, data));
    }

    private void restoreDeletedTeacher(AppUser user, Teacher teacher, TargetRoleProfileData data) {
        castIfPresent(user, data)
                .ifPresentOrElse(
                        form -> restoreDeletedTeacherWithSubmittedData(teacher, form),
                        () -> restoreDeletedTeacherWithPreviousData(teacher)
                );
    }

    private void restoreDeletedTeacherWithPreviousData(Teacher teacher) {
        teacher.setDeletedAt(null);
    }

    private void restoreDeletedTeacherWithSubmittedData(Teacher teacher, ToTeacherRoleProfileData data) {
        teacher.setAcademicRank(data.academicRank());
        teacher.setOffice(data.office());
        teacher.setDeletedAt(null);
    }

    private void createTeacher(AppUser user, ToTeacherRoleProfileData data) {
        var teacher = Teacher.builder()
                .user(user)
                .academicRank(data.academicRank())
                .office(data.office())
                .build();

        teacherRepository.save(teacher);
    }

    private Optional<ToTeacherRoleProfileData> castIfPresent(AppUser user, TargetRoleProfileData data) {
        return Optional.ofNullable(data).map(value -> {
            if (value instanceof ToTeacherRoleProfileData form) {
                return form;
            }

            throw new RoleChangeException(
                    user.getId(),
                    user.getRole(),
                    UserRole.TEACHER,
                    "Invalid target profile data type for TEACHER: actual=" + value.getClass().getSimpleName()
            );
        });
    }

    private ToTeacherRoleProfileData requireTeacherData(AppUser user, TargetRoleProfileData data) {
        return castIfPresent(user, data)
                .orElseThrow(() -> new RoleChangeException(
                        user.getId(),
                        user.getRole(),
                        UserRole.TEACHER,
                        "Teacher profile data is required."
                ));
    }
}