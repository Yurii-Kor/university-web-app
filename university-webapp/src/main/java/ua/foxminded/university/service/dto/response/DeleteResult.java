package ua.foxminded.university.service.dto.response;

import java.util.Collection;

public record DeleteResult(Collection<Long> deletedIds, Collection<Long> notFoundIds) {
}
