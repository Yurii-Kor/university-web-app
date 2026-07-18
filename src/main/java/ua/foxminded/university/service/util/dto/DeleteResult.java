package ua.foxminded.university.service.util.dto;

import java.util.Collection;

public record DeleteResult(Collection<Long> deletedIds, Collection<Long> notFoundIds) {
}
