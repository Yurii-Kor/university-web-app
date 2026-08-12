package ua.foxminded.university.web.group.cards;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import ua.foxminded.university.model.persistence.studygroup.projection.GroupCardView;

public interface GroupsCardsPageStrategy {

    GroupsCardsPageMode mode();

    Page<GroupCardView> loadGroups(
            long userId,
            Pageable pageable);
}