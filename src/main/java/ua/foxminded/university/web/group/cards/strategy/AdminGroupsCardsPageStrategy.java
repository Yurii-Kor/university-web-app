package ua.foxminded.university.web.group.cards.strategy;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import ua.foxminded.university.model.persistence.studygroup.projection.GroupCardView;
import ua.foxminded.university.service.studygroup.StudyGroupService;
import ua.foxminded.university.web.group.cards.GroupsCardsPageMode;
import ua.foxminded.university.web.group.cards.GroupsCardsPageStrategy;

@Component
@RequiredArgsConstructor
public class AdminGroupsCardsPageStrategy
        implements GroupsCardsPageStrategy {

    private final StudyGroupService groupService;

    @Override
    public GroupsCardsPageMode mode() {
        return GroupsCardsPageMode.ADMIN;
    }

    @Override
    public Page<GroupCardView> loadGroups(
            long userId,
            Pageable pageable) {

        return groupService.listGroupCardsForAdmin(
                pageable);
    }
}