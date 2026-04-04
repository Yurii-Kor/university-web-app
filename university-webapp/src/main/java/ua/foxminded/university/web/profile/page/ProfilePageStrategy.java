package ua.foxminded.university.web.profile.page;

public interface ProfilePageStrategy {
    String roleKey();
    ProfilePageModel build(long userId);
}